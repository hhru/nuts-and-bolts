package ru.hh.nab.datasource;

import com.codahale.metrics.health.HealthCheck;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.sql.SQLException;
import java.util.Properties;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.datasource.DataSourceSettings.DATASOURCE_NAME_FORMAT;
import static ru.hh.nab.datasource.DataSourceSettings.HEALTHCHECK_ENABLED;
import static ru.hh.nab.datasource.DataSourceSettings.HEALTHCHECK_SETTINGS_PREFIX;
import static ru.hh.nab.datasource.DataSourceSettings.ROUTING_SECONDARY_DATASOURCE;
import ru.hh.nab.datasource.healthcheck.HealthCheckHikariDataSource;
import ru.hh.nab.datasource.healthcheck.UnhealthyDataSourceException;
import static ru.hh.nab.datasource.routing.DataSourceContext.onDataSource;
import ru.hh.nab.datasource.routing.DataSourceContextUnsafe;
import ru.hh.nab.datasource.routing.DatabaseSwitcher;
import ru.hh.nab.datasource.routing.RoutingDataSource;
import ru.hh.nab.datasource.routing.RoutingDataSourceFactory;

@SpringBootTest(classes = DataSourceSwitchingTest.DataSourceSwitchingTestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class DataSourceSwitchingTest {

  private static final String DB1 = "db1";
  private static final String DB2 = "db2";
  private static final String CRON_MASTER = "cronMaster";

  @Inject
  private DataSourceFactory dataSourceFactory;
  @Inject
  private RoutingDataSourceFactory routingDataSourceFactory;
  @Inject
  private DatabaseNameHolder databaseNameHolder;

  @Inject
  private DataSource routingDataSource;

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
  }

  @AfterAll
  public static void tearDown() {
    DataSourceContextUnsafe.setDatabaseSwitcher(null);
    DataSourcePropertiesStorage.clear();
  }

  @Test
  public void testRoutingToDb1ReadOnly() throws Exception {
    databaseNameHolder.setDatabaseName(DB1);

    invokeMethodOnDataSource(DataSourceType.READONLY);

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
  public void testRoutingToDb1Master() throws SQLException {
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
  public void testRoutingToDb2ReadOnly() throws Exception {
    databaseNameHolder.setDatabaseName(DB2);

    invokeMethodOnDataSource(DataSourceType.READONLY);

    verify(db1MasterDataSource, never()).getConnection();
    verify(db1ReadOnlyDataSource, never()).getConnection();
    verify(db1SlowDataSource, never()).getConnection();
    verify(db1CronMasterDataSource, never()).getConnection();

    verify(db2MasterDataSource, never()).getConnection();
    verify(db2ReadOnlyDataSource, times(1)).getConnection();
    verify(db2SlowDataSource, never()).getConnection();
    verify(db2CronMasterDataSource, never()).getConnection();
  }

  @Test
  public void testRoutingToDb2Master() throws SQLException {
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
    databaseNameHolder.setDatabaseName(DB1);

    doThrow(UnhealthyDataSourceException.class).when(db1SlowDataSource).getConnection();
    RuntimeException ex = assertThrows(RuntimeException.class, () -> invokeMethodOnDataSource(DataSourceType.SLOW));
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
    RuntimeException ex = assertThrows(RuntimeException.class, () -> invokeMethodOnDataSource(DataSourceType.SLOW));
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
    databaseNameHolder.setDatabaseName(DB1);

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

  private void invokeMethodOnDataSource(String dataSource) {
    onDataSource(dataSource, () -> {
      try {
        return routingDataSource.getConnection();
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Configuration
  static class DataSourceSwitchingTestConfig {

    @Bean
    DatabaseSwitcher databaseSwitcher(DatabaseNameHolder databaseNameHolder) {
      return new DatabaseSwitcher(databaseNameHolder::getDatabaseName);
    }

    @Bean
    DatabaseNameHolder databaseNameHolder() {
      return new DatabaseNameHolder();
    }

    @Bean
    DataSourceFactory dataSourceFactory(DatabaseSwitcher databaseSwitcher) {
      return new TestDataSourceFactory(databaseSwitcher);
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

    private static Properties createProperties(String dataSourceName) {
      Properties properties = new Properties();
      properties.setProperty(dataSourceName + ".pool.maximumPoolSize", "2");
      return properties;
    }

    private static DataSource createDsSpy(DataSourceFactory dataSourceFactory, String databaseName, String dataSourceType, Properties properties) {
      try {
        DataSource dataSource = spy(dataSourceFactory.create(databaseName, dataSourceType, false, new FileSettings(properties)));
        var healthCheckHikariDataSource = dataSource.unwrap(HealthCheckHikariDataSource.class);
        if (healthCheckHikariDataSource != null) {
          HealthCheckHikariDataSource.AsyncHealthCheckDecorator failedHealthCheck = mock(HealthCheckHikariDataSource.AsyncHealthCheckDecorator.class);
          when(failedHealthCheck.check()).thenReturn(HealthCheck.Result.unhealthy("Data source is unhealthy"));
          when(healthCheckHikariDataSource.getHealthCheck()).thenReturn(failedHealthCheck);
        }
        return dataSource;
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }

  static class DatabaseNameHolder {

    private String databaseName;

    public String getDatabaseName() {
      return databaseName;
    }

    public void setDatabaseName(String databaseName) {
      this.databaseName = databaseName;
    }
  }
}
