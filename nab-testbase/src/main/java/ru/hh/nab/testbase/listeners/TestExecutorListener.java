package ru.hh.nab.testbase.listeners;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.starter.NabCommonConfig;
import ru.hh.nab.testbase.listeners.dto.ResultStatus;
import ru.hh.nab.testbase.listeners.dto.StandType;
import ru.hh.nab.testbase.listeners.dto.TestExecResult;

public class TestExecutorListener implements TestExecutionListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestExecutorListener.class);
  private static final String SERVICE_NAME_PROPERTY_FILE = "service-test.properties";
  private static final String DRIVER_NAME = "org.postgresql.Driver";
  private static final String LAUNCH_DATA_SQL = "insert into unit_tests_launch_info (server_name, service_name, " +
      "branch_name, unique_launch_id, start_time, end_time) VALUES ('%s', '%s', '%s', %s, '%s', '%s') returning unit_tests_launch_info_id";
  private static final String RUN_DATA_SQL = "INSERT INTO unit_tests_stats (unit_tests_launch_info_id, class_name, test_name, " +
      "status, duration, message) VALUES (?, ?, ?, ?::unit_test_status, ?, ?)";
  private static final int INSERT_BATCH_SIZE = 1000;
  private final Map<String, TestExecResult> execResultMap;
  private final LocalDateTime startTime;
  private boolean canUseDB;

  public TestExecutorListener() {
    try {
      Class.forName(DRIVER_NAME);
      canUseDB = true;
    } catch (ClassNotFoundException e) {
      LOGGER.warn("Failed to load '{}' for junit test executor listener", DRIVER_NAME, e);
      canUseDB = false;
    }
    startTime = LocalDateTime.now();
    execResultMap = new ConcurrentHashMap<>();
  }

  @Override
  public void testPlanExecutionFinished(TestPlan testPlan) {
    StandType standType = getStandType();
    Connection connection = getNewConnection();
    String serviceName = (String) loadProperties().get(NabCommonConfig.SERVICE_NAME_PROPERTY);
    if (connection == null || standType == null || serviceName == null || serviceName.length() == 0) {
      return;
    }
    try (PreparedStatement preparedStatement = connection.prepareStatement(RUN_DATA_SQL);
         Statement statement = connection.createStatement()) {
      connection.setAutoCommit(false);
      int launchId = sendLaunchDataAndGetId(serviceName, statement);
      sendExecutionData(preparedStatement, launchId);
      connection.commit();
      connection.setAutoCommit(true);
    } catch (SQLException e) {
      LOGGER.error("Fail to insert test execution data, making rollback", e);
      tryRollbackOrLogException(connection);
    } finally {
      tryCloseOrLogException(connection);
    }
  }

  @Override
  public void executionSkipped(TestIdentifier testIdentifier, String reason) {
    TestExecResult testExecResult = getTestExecResult(testIdentifier);
    testExecResult.setEndTime(testExecResult.getStartTime());
    testExecResult.setStatus(ResultStatus.SKIPPED);
    execResultMap.put(testIdentifier.getUniqueId(), testExecResult);
  }

  @Override
  public void executionStarted(TestIdentifier testIdentifier) {
    if (testIdentifier.getType().isContainer()) {
      return;
    }
    execResultMap.put(testIdentifier.getUniqueId(), getTestExecResult(testIdentifier));
  }

  @Override
  public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
    if (testIdentifier.getType().isContainer()) {
      return;
    }
    TestExecResult testExecResult = execResultMap.get(testIdentifier.getUniqueId());
    testExecResult.setEndTime(Instant.now());
    switch (testExecutionResult.getStatus()) {
      case ABORTED:
        return;
      case FAILED:
        testExecResult.setStatus(ResultStatus.FAILED);
        Optional<Throwable> optionalThrowable = testExecutionResult.getThrowable();
        optionalThrowable.ifPresent(throwable -> testExecResult.setMessage(throwable.getMessage()));
        break;
      case SUCCESSFUL:
        testExecResult.setStatus(ResultStatus.SUCCESSFUL);
        break;
    }
  }

  private TestExecResult getTestExecResult(TestIdentifier testIdentifier) {
    Optional<TestSource> source = testIdentifier.getSource();
    String className = testIdentifier.getParentId().orElse("noname");
    String methodName = testIdentifier.getDisplayName();
    if (source.isPresent() && source.get() instanceof MethodSource) {
      MethodSource methodSource = (MethodSource) source.get();
      className = methodSource.getClassName();
      methodName = methodSource.getMethodName();
    }
    return new TestExecResult(className, methodName);
  }

  private int sendLaunchDataAndGetId(String serviceName, Statement statement) throws SQLException {
    String uniqueLaunchId = System.getProperty("uniqueLaunchId");
    String query = String.format(
        LAUNCH_DATA_SQL,
        getComputerName(),
        serviceName,
        getBuildBranch(),
        (uniqueLaunchId == null) ? null : String.format("'%s'", uniqueLaunchId),
        startTime.toInstant(ZoneOffset.UTC),
        LocalDateTime.now().toInstant(ZoneOffset.UTC)
    );
    ResultSet resultSet = statement.executeQuery(query);
    resultSet.next();
    return resultSet.getInt(1);
  }

  private void sendExecutionData(PreparedStatement preparedStatement, int launchId) throws SQLException {
    int count = 0;
    for (TestExecResult result : execResultMap.values()) {
      preparedStatement.setInt(1, launchId);
      preparedStatement.setString(2, result.getClassName());
      preparedStatement.setString(3, result.getMethodName());
      preparedStatement.setString(4, result.getStatus().name());
      preparedStatement.setLong(5, Duration.between(result.getStartTime(), result.getEndTime()).toMillis());
      if (result.getMessage() == null) {
        preparedStatement.setNull(6, Types.VARCHAR);
      } else {
        preparedStatement.setString(6, result.getMessage());
      }
      preparedStatement.addBatch();
      if (++count % INSERT_BATCH_SIZE == 0) {
        preparedStatement.executeBatch();
      }
    }
    preparedStatement.executeBatch();
  }

  public Connection getNewConnection() {
    if (canUseDB) {
      try {
        return DriverManager.getConnection(
            getDbUrl(),
            getEnvOrDefault("TEST_DB_LOGIN", "at_stats"),
            getEnvOrDefault("TEST_DB_PASSWORD", "at_stats_password")
        );
      } catch (SQLException | RuntimeException e) {
        LOGGER.warn("Failed to create DB connection for junit test executor listener");
      }
    }
    return null;
  }

  private StandType getStandType() {
    try {
      String stand_type = System.getenv("STAND_TYPE");
      if (stand_type == null || stand_type.isBlank()) {
        return null;
      }
      return StandType.valueOf(stand_type.toUpperCase());
    } catch (IllegalArgumentException e) {
      LOGGER.warn("Fail to get stand type");
    }
    return null;
  }

  private String getDbUrl() {
    return String.format("jdbc:postgresql://%s:%s/%s",
        getEnvOrDefault("TEST_DB_HOST", "10.208.30.79"),
        getEnvOrDefault("TEST_DB_PORT", "5432"),
        getEnvOrDefault("TEST_DB_NAME", "at_stats")
    );
  }

  public String getBuildBranch() {
    String buildBranch = System.getenv("BUILD_BRANCH");
    if (buildBranch != null) {
      return buildBranch;
    }
    try {
      Process process = Runtime.getRuntime().exec("git rev-parse --abbrev-ref HEAD");
      process.waitFor();
      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      return reader.readLine();
    } catch (IOException e) {
      System.out.printf("Couldn't get current git branch from test listener \n %s", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return "unknown";
  }

  private String getComputerName() {
    String hostName = System.getenv("HOST_NAME");
    if (hostName == null) {
      try {
        hostName = InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException e) {
        hostName = "unknown";
      }
    }
    return hostName;
  }

  public Properties loadProperties() {
    Properties prop = new Properties();
    try (InputStream inputStream = this.getClass().getResourceAsStream("/" + SERVICE_NAME_PROPERTY_FILE)) {
      prop.load(inputStream);
    } catch (IOException e) {
      LOGGER.warn("Fail to read '{}' properties file", SERVICE_NAME_PROPERTY_FILE, e);
    }
    return prop;
  }

  private String getEnvOrDefault(String env, String def) {
    String value = System.getenv(env);
    return (value == null) ? def : value;
  }

  private void tryRollbackOrLogException(Connection connection) {
    try {
      connection.rollback();
    } catch (SQLException t) {
      LOGGER.error("Fail to rollback!", t);
    }
  }

  private void tryCloseOrLogException(Connection connection) {
    try {
      connection.close();
    } catch (SQLException e) {
      LOGGER.error("Fail to close JDBC connection for url '{}'", getDbUrl(), e);
    }
  }
}
