package ru.hh.nab.datasource;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import javax.sql.DataSource;
import org.apache.commons.text.StringSubstitutor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.datasource.DataSourceSettings.HEALTHCHECK_ENABLED;
import static ru.hh.nab.datasource.DataSourceSettings.HEALTHCHECK_SETTINGS_PREFIX;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_LONG_CONNECTION_USAGE_MS;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_SEND_SAMPLED_STATS;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_SEND_STATS;
import static ru.hh.nab.datasource.DataSourceSettings.ROUTING_SECONDARY_DATASOURCE;
import static ru.hh.nab.datasource.DataSourceSettings.STATEMENT_TIMEOUT_MS;
import ru.hh.nab.datasource.healthcheck.HealthCheckHikariDataSource;
import ru.hh.nab.datasource.healthcheck.HealthCheckHikariDataSourceFactory;
import ru.hh.nab.datasource.monitoring.NabMetricsTrackerFactoryProvider;
import ru.hh.nab.datasource.monitoring.StatementTimeoutDataSource;
import ru.hh.nab.jdbc.common.DataSourcePropertiesStorage;
import ru.hh.nab.metrics.StatsDSender;
import static ru.hh.nab.testbase.NabTestConfig.TEST_SERVICE_NAME;
import ru.hh.nab.testbase.postgres.embedded.EmbeddedPostgresDataSourceFactory;

public class DataSourceFactoryTest {
  private static final String TEST_DATA_SOURCE_TYPE = DataSourceType.MASTER;

  private static PostgreSQLContainer<?> testDbContainer;
  private static DataSourceFactory dataSourceFactory;

  @BeforeAll
  public static void setUpClass() {
    testDbContainer = EmbeddedPostgresDataSourceFactory.getEmbeddedPostgres();
    StatsDSender statsDSender = mock(StatsDSender.class);
    dataSourceFactory = new DataSourceFactory(
        new NabMetricsTrackerFactoryProvider(TEST_SERVICE_NAME, statsDSender),
        new HealthCheckHikariDataSourceFactory(TEST_SERVICE_NAME, statsDSender),
        null
    );
  }

  @BeforeEach
  public void clearDataSourcePropertiesStorage() {
    DataSourcePropertiesStorage.clear();
  }

  @Test
  public void testCreateDataSourceWithIncompleteSettings() {
    Properties properties = createIncompleteTestProperties();

    try {
      createTestDataSource(properties);
      fail();
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().contains("master.pool"));
    }
  }

  @Test
  public void testCreateDataSource() throws SQLException {
    Properties properties = createTestProperties();

    HikariDataSource dataSource = createTestDataSource(properties).unwrap(HikariDataSource.class);
    assertEquals(TEST_DATA_SOURCE_TYPE, dataSource.getPoolName());
  }

  @Test
  public void testCreateHealthCheckHikariDataSource() {
    Properties properties = createTestProperties();
    properties.setProperty(getProperty(HEALTHCHECK_SETTINGS_PREFIX + "." + HEALTHCHECK_ENABLED), "true");
    assertSuccessfulUnwrap(createTestDataSource(properties), HealthCheckHikariDataSource.class);
    assertTrue(DataSourcePropertiesStorage.getSecondaryDataSourceName(TEST_DATA_SOURCE_TYPE).isEmpty());
  }

  @Test
  public void testCreateDataSourceWithRoutingProperty() {
    Properties properties = createTestProperties();
    properties.setProperty(getProperty(HEALTHCHECK_SETTINGS_PREFIX + "." + HEALTHCHECK_ENABLED), "true");
    properties.setProperty(getProperty(ROUTING_SECONDARY_DATASOURCE), DataSourceType.READONLY);
    createTestDataSource(properties);

    Optional<String> secondaryDataSource = DataSourcePropertiesStorage.getSecondaryDataSourceName(TEST_DATA_SOURCE_TYPE);
    assertTrue(secondaryDataSource.isPresent());
    assertEquals(DataSourceType.READONLY, secondaryDataSource.get());
  }

  @Test
  public void testCreateDataSourceWithRoutingPropertyAndDisabledHealthCheck() {
    Properties properties = createTestProperties();
    properties.setProperty(getProperty(ROUTING_SECONDARY_DATASOURCE), DataSourceType.READONLY);
    RuntimeException exception = assertThrows(RuntimeException.class, () -> createTestDataSource(properties));
    String expectedMessage = "Exception during master datasource initialization: if routing.failedHealthcheck.secondaryDataSource is configured, " +
        "healthcheck should be enabled. To prevent misconfiguration application startup will be aborted.";
    String actualMessage = exception.getMessage();
    assertEquals(expectedMessage, actualMessage);
    assertTrue(DataSourcePropertiesStorage.getSecondaryDataSourceName(TEST_DATA_SOURCE_TYPE).isEmpty());
  }

  @Test
  public void testCreateStatementTimeoutDataSource() {
    Properties properties = createTestProperties();
    properties.setProperty(getProperty(STATEMENT_TIMEOUT_MS), "100");
    assertSuccessfulUnwrap(createTestDataSource(properties), StatementTimeoutDataSource.class);
  }

  @Test
  public void testCreateDataSourceWithMetrics() throws SQLException {
    Properties properties = createTestProperties();
    properties.setProperty(getProperty(MONITORING_SEND_STATS), "true");
    properties.setProperty(getProperty(MONITORING_LONG_CONNECTION_USAGE_MS), "10");
    properties.setProperty(getProperty(MONITORING_SEND_SAMPLED_STATS), "true");

    HikariDataSource dataSource = createTestDataSource(properties).unwrap(HikariDataSource.class);
    assertNotNull(dataSource.getMetricsTrackerFactory());
  }

  private void assertSuccessfulUnwrap(DataSource dataSource, Class<?> clazz) {
    try {
      dataSource.unwrap(clazz);
    } catch (SQLException e) {
      fail(() -> "Unable to unwrap to " + clazz.getSimpleName());
    }
  }

  private static DataSource createTestDataSource(Properties properties) {
    return dataSourceFactory.create(TEST_DATA_SOURCE_TYPE, false, new FileSettings(properties));
  }

  private static Properties createTestProperties() {
    Properties properties = createIncompleteTestProperties();
    properties.setProperty(getProperty(DataSourceSettings.POOL_SETTINGS_PREFIX + ".maximumPoolSize"), "2");
    return properties;
  }

  private static Properties createIncompleteTestProperties() {
    Properties properties = new Properties();

    final StringSubstitutor jdbcUrlParamsSubstitutor = new StringSubstitutor(Map.of(
        "port", testDbContainer.getFirstMappedPort(),
        "host", testDbContainer.getHost(),
        "database", EmbeddedPostgresDataSourceFactory.DEFAULT_DATABASE
    ));
    properties.setProperty(
        getProperty(DataSourceSettings.JDBC_URL),
        jdbcUrlParamsSubstitutor.replace(EmbeddedPostgresDataSourceFactory.DEFAULT_JDBC_URL)
    );

    properties.setProperty(getProperty(DataSourceSettings.USER), EmbeddedPostgresDataSourceFactory.DEFAULT_USER);
    properties.setProperty(getProperty(DataSourceSettings.PASSWORD), EmbeddedPostgresDataSourceFactory.DEFAULT_PASSWORD);
    return properties;
  }

  private static String getProperty(String propertyName) {
    return String.format("%s.%s", TEST_DATA_SOURCE_TYPE, propertyName);
  }

  private static String getMetricName(String metricName) {
    return String.format("%s.%s.%s", TEST_SERVICE_NAME, TEST_DATA_SOURCE_TYPE, metricName);
  }
}
