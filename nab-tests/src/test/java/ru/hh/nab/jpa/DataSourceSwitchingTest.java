package ru.hh.nab.jpa;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.PersistenceException;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import ru.hh.nab.datasource.DataSourceFactory;
import static ru.hh.nab.datasource.DataSourceSettings.HEALTHCHECK_ENABLED;
import static ru.hh.nab.datasource.DataSourceSettings.HEALTHCHECK_SETTINGS_PREFIX;
import static ru.hh.nab.datasource.DataSourceSettings.ROUTING_SECONDARY_DATASOURCE;
import ru.hh.nab.datasource.DatabaseSwitcherImpl;
import static ru.hh.nab.datasource.DatabaseSwitcherImpl.DATASOURCE_NAME_FORMAT;
import ru.hh.nab.datasource.healthcheck.AsyncHealthCheck;
import ru.hh.nab.datasource.healthcheck.HealthCheckHikariDataSource;
import ru.hh.nab.datasource.healthcheck.HealthCheckHikariDataSourceFactory;
import ru.hh.nab.datasource.healthcheck.HealthCheckResultImpl;
import ru.hh.nab.datasource.healthcheck.UnhealthyDataSourceException;
import ru.hh.nab.datasource.monitoring.NabMetricsTrackerFactoryProvider;
import ru.hh.nab.hibernate.datasource.RoutingDataSourceFactory;
import ru.hh.nab.jdbc.common.DataSourcePropertiesStorage;
import ru.hh.nab.jdbc.common.DataSourceType;
import ru.hh.nab.jdbc.common.DatabaseSwitcher;
import ru.hh.nab.jdbc.common.healthcheck.HealthCheckResult;
import static ru.hh.nab.jdbc.routing.DataSourceContext.onDataSource;
import ru.hh.nab.jdbc.routing.DataSourceContextUnsafe;
import ru.hh.nab.jdbc.routing.RoutingDataSource;
import ru.hh.nab.jpa.model.TestEntity;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.testbase.jpa.JpaTestBase;
import ru.hh.nab.testbase.postgres.embedded.EmbeddedPostgresDataSourceFactory;

@ContextConfiguration(
    classes = {
        JpaTestConfig.class,
        DataSourceSwitchingTest.DataSourceSwitchingTestConfig.class
    }
)
public class DataSourceSwitchingTest extends JpaTestBase {

  private static final String DB1 = "db1";
  private static final String DB2 = "db2";
  private static final String CRON_MASTER = "cronMaster";

  @Inject
  private TransactionalScope transactionalScope;
  @Inject
  private DataSourceFactory dataSourceFactory;
  @Inject
  private RoutingDataSourceFactory routingDataSourceFactory;
  @Inject
  private DatabaseNameHolder databaseNameHolder;

  @Inject
  @Named("db1MasterDataSource")
  private DataSource db1MasterDataSource;
  @Inject
  @Named("db1ReadOnlyDataSource")
  private DataSource db1ReadOnlyDataSource;
  @Inject
  @Named("db1SlowDataSource")
  private DataSource db1SlowDataSource;
  @Inject
  @Named("db1CronMasterDataSource")
  private DataSource db1CronMasterDataSource;

  @Inject
  @Named("db2MasterDataSource")
  private DataSource db2MasterDataSource;
  @Inject
  @Named("db2ReadOnlyDataSource")
  private DataSource db2ReadOnlyDataSource;
  @Inject
  @Named("db2SlowDataSource")
  private DataSource db2SlowDataSource;
  @Inject
  @Named("db2CronMasterDataSource")
  private DataSource db2CronMasterDataSource;

  @BeforeEach
  public void setUp() {
    reset(db1MasterDataSource);
    reset(db1ReadOnlyDataSource);
    reset(db1SlowDataSource);
    reset(db1CronMasterDataSource);

    reset(db2MasterDataSource);
    reset(db2ReadOnlyDataSource);
    reset(db2SlowDataSource);
    reset(db2CronMasterDataSource);

    databaseNameHolder.reset();
  }

  @AfterAll
  public static void tearDown() {
    DataSourceContextUnsafe.setDatabaseSwitcher(null);
    DataSourcePropertiesStorage.clear();
  }

  @Test
  public void testDsManageInsideTxScope() throws Exception {
    Executor executor = Executors.newFixedThreadPool(1);
    CompletableFuture
        .supplyAsync(
            () -> {
              Supplier<TestEntity> supplier = () -> entityManager.find(TestEntity.class, 1);
              TargetMethod<TestEntity> method = () -> onDataSource(DataSourceType.READONLY, supplier);
              return transactionalScope.read(method);
            },
            executor
        )
        .get();

    verify(db1MasterDataSource, never()).getConnection();
    verify(db1ReadOnlyDataSource, times(1)).getConnection();
    verify(db1SlowDataSource, never()).getConnection();
    verify(db1CronMasterDataSource, never()).getConnection();

    verify(db2MasterDataSource, never()).getConnection();
    verify(db2ReadOnlyDataSource, never()).getConnection();
    verify(db2SlowDataSource, never()).getConnection();
    verify(db2CronMasterDataSource, never()).getConnection();
  }

  @Test
  public void testTxScopeDoesntChangeDs() throws Exception {
    Executor executor = Executors.newFixedThreadPool(1);
    CompletableFuture.supplyAsync(() -> invokeMethodOnDataSource(DataSourceType.READONLY), executor).get();

    verify(db1MasterDataSource, never()).getConnection();
    verify(db1ReadOnlyDataSource, times(1)).getConnection();
    verify(db1SlowDataSource, never()).getConnection();
    verify(db1CronMasterDataSource, never()).getConnection();

    verify(db2MasterDataSource, never()).getConnection();
    verify(db2ReadOnlyDataSource, never()).getConnection();
    verify(db2SlowDataSource, never()).getConnection();
    verify(db2CronMasterDataSource, never()).getConnection();
  }

  @Test
  public void testRoutingToDb1() throws SQLException {
    databaseNameHolder.setDatabaseName(DB1);
    invokeMethodOnDataSource(DataSourceType.MASTER);

    verify(db1MasterDataSource, times(1)).getConnection();
    verify(db1ReadOnlyDataSource, never()).getConnection();
    verify(db1SlowDataSource, never()).getConnection();
    verify(db1CronMasterDataSource, never()).getConnection();

    verify(db2MasterDataSource, never()).getConnection();
    verify(db2ReadOnlyDataSource, never()).getConnection();
    verify(db2SlowDataSource, never()).getConnection();
    verify(db2CronMasterDataSource, never()).getConnection();
  }

  @Test
  public void testRoutingToDb2() throws SQLException {
    databaseNameHolder.setDatabaseName(DB2);
    invokeMethodOnDataSource(DataSourceType.MASTER);

    verify(db1MasterDataSource, never()).getConnection();
    verify(db1ReadOnlyDataSource, never()).getConnection();
    verify(db1SlowDataSource, never()).getConnection();
    verify(db1CronMasterDataSource, never()).getConnection();

    verify(db2MasterDataSource, times(1)).getConnection();
    verify(db2ReadOnlyDataSource, never()).getConnection();
    verify(db2SlowDataSource, never()).getConnection();
    verify(db2CronMasterDataSource, never()).getConnection();
  }

  @Test
  public void testSwitchingIfSecondaryDataSourceIsNotPresentOnDb1() throws Exception {
    doThrow(UnhealthyDataSourceException.class).when(db1SlowDataSource).getConnection();
    Exception ex = invokeMethodOnDataSourceWithException(DataSourceType.SLOW);
    assertTrue(ex.getCause() instanceof UnhealthyDataSourceException);

    verify(db1MasterDataSource, never()).getConnection();
    verify(db1ReadOnlyDataSource, never()).getConnection();
    verify(db1SlowDataSource, times(1)).getConnection();
    verify(db1CronMasterDataSource, never()).getConnection();

    verify(db2MasterDataSource, never()).getConnection();
    verify(db2ReadOnlyDataSource, never()).getConnection();
    verify(db2SlowDataSource, never()).getConnection();
    verify(db2CronMasterDataSource, never()).getConnection();
  }

  @Test
  public void testSwitchingIfSecondaryDataSourceIsNotPresentOnDb2() throws Exception {
    databaseNameHolder.setDatabaseName(DB2);

    doThrow(UnhealthyDataSourceException.class).when(db2SlowDataSource).getConnection();
    Exception ex = invokeMethodOnDataSourceWithException(DataSourceType.SLOW);
    assertTrue(ex.getCause() instanceof UnhealthyDataSourceException);

    verify(db1MasterDataSource, never()).getConnection();
    verify(db1ReadOnlyDataSource, never()).getConnection();
    verify(db1SlowDataSource, never()).getConnection();
    verify(db1CronMasterDataSource, never()).getConnection();

    verify(db2MasterDataSource, never()).getConnection();
    verify(db2ReadOnlyDataSource, never()).getConnection();
    verify(db2SlowDataSource, times(1)).getConnection();
    verify(db2CronMasterDataSource, never()).getConnection();
  }

  @Test
  public void testSwitchingIfSecondaryDataSourceIsPresentOnDb1() throws Exception {
    doThrow(UnhealthyDataSourceException.class).when(db1CronMasterDataSource).getConnection();
    invokeMethodOnDataSource(CRON_MASTER);

    verify(db1MasterDataSource, times(1)).getConnection();
    verify(db1ReadOnlyDataSource, never()).getConnection();
    verify(db1SlowDataSource, never()).getConnection();
    verify(db1CronMasterDataSource, never()).getConnection();

    verify(db2MasterDataSource, never()).getConnection();
    verify(db2ReadOnlyDataSource, never()).getConnection();
    verify(db2SlowDataSource, never()).getConnection();
    verify(db2CronMasterDataSource, never()).getConnection();
  }

  @Test
  public void testSwitchingIfSecondaryDataSourceIsPresentOnDb2() throws Exception {
    databaseNameHolder.setDatabaseName(DB2);

    doThrow(UnhealthyDataSourceException.class).when(db2CronMasterDataSource).getConnection();
    invokeMethodOnDataSource(CRON_MASTER);

    verify(db1MasterDataSource, never()).getConnection();
    verify(db1ReadOnlyDataSource, never()).getConnection();
    verify(db1SlowDataSource, never()).getConnection();
    verify(db1CronMasterDataSource, never()).getConnection();

    verify(db2MasterDataSource, times(1)).getConnection();
    verify(db2ReadOnlyDataSource, never()).getConnection();
    verify(db2SlowDataSource, never()).getConnection();
    verify(db2CronMasterDataSource, never()).getConnection();
  }

  @Test
  public void testSecondaryDataSourceIsNotConfiguredValidation() {
    String dataSourceName = CRON_MASTER;
    Properties properties = DataSourceSwitchingTestConfig.createProperties(dataSourceName);
    properties.setProperty(dataSourceName + "." + HEALTHCHECK_SETTINGS_PREFIX + "." + HEALTHCHECK_ENABLED, "true");
    properties.setProperty(dataSourceName + "." + ROUTING_SECONDARY_DATASOURCE, DataSourceType.MASTER);
    DataSource dataSource = dataSourceFactory.create(CRON_MASTER, false, new FileSettings(properties));

    RoutingDataSource routingDataSource = routingDataSourceFactory.create();
    routingDataSource.addNamedDataSource(dataSource);
    IllegalStateException ex = assertThrows(IllegalStateException.class, routingDataSource::afterPropertiesSet);
    assertEquals("Secondary datasource master is not configured", ex.getMessage());
  }

  private TestEntity invokeMethodOnDataSource(String dataSource) {
    TargetMethod<TestEntity> method = () -> entityManager.find(TestEntity.class, 1);
    return onDataSource(dataSource, () -> transactionalScope.read(method));
  }

  private Exception invokeMethodOnDataSourceWithException(String dataSource) {
    TargetMethod<TestEntity> method = () -> entityManager.find(TestEntity.class, 1);
    PersistenceException ex = assertThrows(
        PersistenceException.class,
        () -> onDataSource(dataSource, () -> transactionalScope.read(method))
    );
    return ex;
  }

  @Configuration
  static class DataSourceSwitchingTestConfig {
    static final String SERVICE_NAME_VALUE = "test-service";

    @Bean
    StatsDSender statsDSender() {
      return mock(StatsDSender.class);
    }

    @Bean
    DataSourceFactory dataSourceFactory(StatsDSender statsDSender, DatabaseSwitcher databaseSwitcher) {
      return new EmbeddedPostgresDataSourceFactory(
          new NabMetricsTrackerFactoryProvider(SERVICE_NAME_VALUE, statsDSender),
          new HealthCheckHikariDataSourceFactory(SERVICE_NAME_VALUE, statsDSender) {
            @Override
            public HikariDataSource create(HikariConfig hikariConfig) {
              return spy(super.create(hikariConfig));
            }
          },
          databaseSwitcher
      );
    }

    @Bean
    RoutingDataSourceFactory routingDataSourceFactory() {
      return new RoutingDataSourceFactory(null, null);
    }

    @Bean
    DataSource db1MasterDataSource(DataSourceFactory dataSourceFactory) {
      String dataSourceName = DATASOURCE_NAME_FORMAT.formatted(DB1, DataSourceType.MASTER);
      Properties properties = createProperties(dataSourceName);
      return createDsSpy(dataSourceFactory, DB1, DataSourceType.MASTER, properties);
    }

    @Bean
    DataSource db1ReadOnlyDataSource(DataSourceFactory dataSourceFactory) {
      String dataSourceName = DATASOURCE_NAME_FORMAT.formatted(DB1, DataSourceType.READONLY);
      Properties properties = createProperties(dataSourceName);
      return createDsSpy(dataSourceFactory, DB1, DataSourceType.READONLY, properties);
    }

    @Bean
    DataSource db1SlowDataSource(DataSourceFactory dataSourceFactory) {
      String dataSourceName = DATASOURCE_NAME_FORMAT.formatted(DB1, DataSourceType.SLOW);
      Properties properties = createProperties(dataSourceName);
      properties.setProperty(dataSourceName + "." + HEALTHCHECK_SETTINGS_PREFIX + "." + HEALTHCHECK_ENABLED, "true");
      return createDsSpy(dataSourceFactory, DB1, DataSourceType.SLOW, properties);
    }

    @Bean
    DataSource db1CronMasterDataSource(DataSourceFactory dataSourceFactory) {
      String dataSourceName = DATASOURCE_NAME_FORMAT.formatted(DB1, CRON_MASTER);
      Properties properties = createProperties(dataSourceName);
      properties.setProperty(dataSourceName + "." + HEALTHCHECK_SETTINGS_PREFIX + "." + HEALTHCHECK_ENABLED, "true");
      properties.setProperty(dataSourceName + "." + ROUTING_SECONDARY_DATASOURCE, DATASOURCE_NAME_FORMAT.formatted(DB1, DataSourceType.MASTER));
      return createDsSpy(dataSourceFactory, DB1, CRON_MASTER, properties);
    }

    @Bean
    DataSource db2MasterDataSource(DataSourceFactory dataSourceFactory) {
      String dataSourceName = DATASOURCE_NAME_FORMAT.formatted(DB2, DataSourceType.MASTER);
      Properties properties = createProperties(dataSourceName);
      return createDsSpy(dataSourceFactory, DB2, DataSourceType.MASTER, properties);
    }

    @Bean
    DataSource db2ReadOnlyDataSource(DataSourceFactory dataSourceFactory) {
      String dataSourceName = DATASOURCE_NAME_FORMAT.formatted(DB2, DataSourceType.READONLY);
      Properties properties = createProperties(dataSourceName);
      return createDsSpy(dataSourceFactory, DB2, DataSourceType.READONLY, properties);
    }

    @Bean
    DataSource db2SlowDataSource(DataSourceFactory dataSourceFactory) {
      String dataSourceName = DATASOURCE_NAME_FORMAT.formatted(DB2, DataSourceType.SLOW);
      Properties properties = createProperties(dataSourceName);
      properties.setProperty(dataSourceName + "." + HEALTHCHECK_SETTINGS_PREFIX + "." + HEALTHCHECK_ENABLED, "true");
      return createDsSpy(dataSourceFactory, DB2, DataSourceType.SLOW, properties);
    }

    @Bean
    DataSource db2CronMasterDataSource(DataSourceFactory dataSourceFactory) {
      String dataSourceName = DATASOURCE_NAME_FORMAT.formatted(DB2, CRON_MASTER);
      Properties properties = createProperties(dataSourceName);
      properties.setProperty(dataSourceName + "." + HEALTHCHECK_SETTINGS_PREFIX + "." + HEALTHCHECK_ENABLED, "true");
      properties.setProperty(dataSourceName + "." + ROUTING_SECONDARY_DATASOURCE, DATASOURCE_NAME_FORMAT.formatted(DB2, DataSourceType.MASTER));
      return createDsSpy(dataSourceFactory, DB2, CRON_MASTER, properties);
    }

    @Primary
    @Bean
    RoutingDataSource dataSource(
        RoutingDataSourceFactory routingDataSourceFactory,

        DataSource db1MasterDataSource,
        DataSource db1ReadOnlyDataSource,
        DataSource db1SlowDataSource,
        DataSource db1CronMasterDataSource,

        DataSource db2MasterDataSource,
        DataSource db2ReadOnlyDataSource,
        DataSource db2SlowDataSource,
        DataSource db2CronMasterDataSource
    ) {
      RoutingDataSource routingDataSource = routingDataSourceFactory.create();
      routingDataSource.addNamedDataSource(db1MasterDataSource);
      routingDataSource.addNamedDataSource(db1ReadOnlyDataSource);
      routingDataSource.addNamedDataSource(db1SlowDataSource);
      routingDataSource.addNamedDataSource(db1CronMasterDataSource);

      routingDataSource.addNamedDataSource(db2MasterDataSource);
      routingDataSource.addNamedDataSource(db2ReadOnlyDataSource);
      routingDataSource.addNamedDataSource(db2SlowDataSource);
      routingDataSource.addNamedDataSource(db2CronMasterDataSource);
      return routingDataSource;
    }

    @Bean
    DatabaseSwitcher databaseSwitcher(DatabaseNameHolder databaseNameHolder) {
      return new DatabaseSwitcherImpl(databaseNameHolder::getDatabaseName);
    }

    @Bean
    MappingConfig mappingConfig() {
      return new MappingConfig(TestEntity.class);
    }

    @Bean
    TransactionalScope transactionalScope() {
      return new TransactionalScope();
    }

    @Bean
    DatabaseNameHolder databaseNameHolder() {
      return new DatabaseNameHolder();
    }

    private static Properties createProperties(String dataSourceName) {
      Properties properties = new Properties();
      properties.setProperty(dataSourceName + ".pool.maximumPoolSize", "2");
      return properties;
    }

    private static DataSource createDsSpy(DataSourceFactory dataSourceFactory, String databaseName, String dataSourceType, Properties properties) {
      DataSource dataSource = spy(dataSourceFactory.create(databaseName, dataSourceType, false, new FileSettings(properties)));
      try {
        var healthCheckHikariDataSource = dataSource.unwrap(HealthCheckHikariDataSource.class);
        AsyncHealthCheck failedHealthCheck = mock(AsyncHealthCheck.class);
        HealthCheckResult healthCheckResult = new HealthCheckResultImpl(false);
        when(failedHealthCheck.getCheckResult()).thenReturn(healthCheckResult);
        when(healthCheckHikariDataSource.getHealthCheck()).thenReturn(failedHealthCheck);
      } catch (SQLException e) {
        // empty
      }
      return dataSource;
    }

    @Named(SERVICE_NAME)
    @Bean(SERVICE_NAME)
    String serviceName() {
      return SERVICE_NAME_VALUE;
    }
  }

  @FunctionalInterface
  interface TargetMethod<T> {
    T invoke();
  }

  static class TransactionalScope {

    @Transactional(readOnly = true)
    public <T> T read(TargetMethod<T> method) {
      return method.invoke();
    }

    @Transactional
    public <T> T write(TargetMethod<T> method) {
      return method.invoke();
    }
  }

  static class DatabaseNameHolder {

    private String databaseName = DB1;

    public void reset() {
      databaseName = DB1;
    }

    public String getDatabaseName() {
      return databaseName;
    }

    public void setDatabaseName(String databaseName) {
      this.databaseName = databaseName;
    }
  }
}
