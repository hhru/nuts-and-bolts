package ru.hh.nab.datasource;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import com.zaxxer.hikari.util.DriverDataSource;
import com.zaxxer.hikari.util.PropertyElf;
import static com.zaxxer.hikari.util.UtilityElf.createInstance;
import static java.lang.Integer.parseInt;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import java.util.Properties;
import javax.annotation.Nullable;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.datasource.DataSourceSettings.DEFAULT_VALIDATION_TIMEOUT_RATIO;
import static ru.hh.nab.datasource.DataSourceSettings.HEALTHCHECK_ENABLED;
import static ru.hh.nab.datasource.DataSourceSettings.HEALTHCHECK_SETTINGS_PREFIX;
import static ru.hh.nab.datasource.DataSourceSettings.JDBC_URL;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_SEND_STATS;
import static ru.hh.nab.datasource.DataSourceSettings.PASSWORD;
import static ru.hh.nab.datasource.DataSourceSettings.POOL_SETTINGS_PREFIX;
import static ru.hh.nab.datasource.DataSourceSettings.ROUTING_SECONDARY_DATASOURCE;
import static ru.hh.nab.datasource.DataSourceSettings.STATEMENT_TIMEOUT_MS;
import static ru.hh.nab.datasource.DataSourceSettings.USER;
import ru.hh.nab.datasource.ext.OpenTelemetryJdbcExtension;
import ru.hh.nab.datasource.healthcheck.HealthCheckHikariDataSourceFactory;
import ru.hh.nab.datasource.monitoring.MetricsTrackerFactoryProvider;
import ru.hh.nab.datasource.monitoring.StatementTimeoutDataSource;

public class DataSourceFactory {
  private static final int HIKARI_MIN_VALIDATION_TIMEOUT_MS = 250;

  private final MetricsTrackerFactoryProvider<?> metricsTrackerFactoryProvider;
  @Nullable
  private final HealthCheckHikariDataSourceFactory healthCheckHikariDataSourceFactory;
  @Nullable
  private final OpenTelemetryJdbcExtension openTelemetryJdbcExtension;

  public DataSourceFactory(
      MetricsTrackerFactoryProvider<?> metricsTrackerFactoryProvider,
      @Nullable HealthCheckHikariDataSourceFactory healthCheckHikariDataSourceFactory,
      @Nullable OpenTelemetryJdbcExtension openTelemetryJdbcExtension
  ) {
    this.metricsTrackerFactoryProvider = metricsTrackerFactoryProvider;
    this.healthCheckHikariDataSourceFactory = healthCheckHikariDataSourceFactory;
    this.openTelemetryJdbcExtension = openTelemetryJdbcExtension;
  }

  public DataSource create(String dataSourceName, boolean isReadonly, FileSettings settings) {
    return createDataSource(dataSourceName, isReadonly, settings.getSubSettings(dataSourceName));
  }

  public DataSource create(HikariConfig hikariConfig, FileSettings dataSourceSettings, boolean isReadonly) {
    boolean sendStats = ofNullable(dataSourceSettings.getBoolean(MONITORING_SEND_STATS)).orElse(false);
    if (sendStats && metricsTrackerFactoryProvider != null) {
      hikariConfig.setMetricsTrackerFactory(metricsTrackerFactoryProvider.create(dataSourceSettings));
    }

    FileSettings healthCheckSettings = dataSourceSettings.getSubSettings(HEALTHCHECK_SETTINGS_PREFIX);
    boolean healthCheckEnabled = healthCheckSettings.getBoolean(HEALTHCHECK_ENABLED, false);
    String secondaryDataSource = dataSourceSettings.getString(ROUTING_SECONDARY_DATASOURCE);
    if (!healthCheckEnabled && secondaryDataSource != null) {
      throw new RuntimeException(String.format(
          "Exception during %s datasource initialization: if %s is configured, healthcheck should be enabled. " +
              "To prevent misconfiguration application startup will be aborted.", hikariConfig.getPoolName(), ROUTING_SECONDARY_DATASOURCE
      ));
    }

    String dataSourceName = hikariConfig.getPoolName();
    DataSource originalDataSource = Optional.ofNullable(hikariConfig.getDataSource()).orElseGet(() -> createOriginalDataSource(hikariConfig));
    DataSource namedDataSource = new NamedDataSource(dataSourceName, originalDataSource);
    DataSource telemetryDataSource = openTelemetryJdbcExtension == null ? namedDataSource : openTelemetryJdbcExtension.wrap(namedDataSource);
    hikariConfig.setDataSource(telemetryDataSource);

    DataSource hikariDataSource;
    if (healthCheckHikariDataSourceFactory != null && healthCheckEnabled) {
      hikariConfig.setHealthCheckRegistry(new HealthCheckRegistry());
      hikariConfig.setHealthCheckProperties(healthCheckSettings.getProperties());
      hikariDataSource = healthCheckHikariDataSourceFactory.create(hikariConfig);
    } else {
      hikariDataSource = new HikariDataSource(hikariConfig);
    }

    String statementTimeoutMsVal = dataSourceSettings.getString(STATEMENT_TIMEOUT_MS);
    if (statementTimeoutMsVal != null) {
      int statementTimeoutMs = parseInt(statementTimeoutMsVal);
      if (statementTimeoutMs > 0) {
        hikariDataSource = new StatementTimeoutDataSource(hikariDataSource, statementTimeoutMs);
      }
    }

    checkDataSource(hikariDataSource, dataSourceName);
    DataSourceType.registerPropertiesFor(hikariConfig.getPoolName(), new DataSourceType.DataSourceProperties(!isReadonly, secondaryDataSource));

    return hikariDataSource;
  }

  protected DataSource createDataSource(String dataSourceName, boolean isReadonly, FileSettings dataSourceSettings) {
    HikariConfig hikariConfig = createBaseHikariConfig(dataSourceName, dataSourceSettings);
    return create(hikariConfig, dataSourceSettings, isReadonly);
  }

  private static HikariConfig createBaseHikariConfig(String dataSourceName, FileSettings dataSourceSettings) {
    Properties poolProperties = dataSourceSettings.getSubProperties(POOL_SETTINGS_PREFIX);
    if (poolProperties.isEmpty()) {
      throw new RuntimeException(String.format(
        "Exception during %1$s pooled datasource initialization: could not find %1$s.%2$s settings in config file. " +
        "To prevent misconfiguration application startup will be aborted.",
        dataSourceName, POOL_SETTINGS_PREFIX
      ));
    }

    HikariConfig config = new HikariConfig(poolProperties);
    config.setJdbcUrl(dataSourceSettings.getString(JDBC_URL));
    config.setUsername(dataSourceSettings.getString(USER));
    config.setPassword(dataSourceSettings.getString(PASSWORD));
    config.setPoolName(dataSourceName);
    config.setReadOnly(false);

    long validationTimeoutMs = Math.round(config.getConnectionTimeout() * DEFAULT_VALIDATION_TIMEOUT_RATIO);
    config.setValidationTimeout(Math.max(validationTimeoutMs, HIKARI_MIN_VALIDATION_TIMEOUT_MS));

    return config;
  }

  private static DataSource createOriginalDataSource(HikariConfig hikariConfig) {
    String jdbcUrl = hikariConfig.getJdbcUrl();
    String username = hikariConfig.getUsername();
    String password = hikariConfig.getPassword();
    String dsClassName = hikariConfig.getDataSourceClassName();
    String driverClassName = hikariConfig.getDriverClassName();
    String dataSourceJNDI = hikariConfig.getDataSourceJNDI();
    Properties dataSourceProperties = hikariConfig.getDataSourceProperties();
    DataSource result = null;
    if (dsClassName != null) {
      result = createInstance(dsClassName, DataSource.class);
      PropertyElf.setTargetFromProperties(result, dataSourceProperties);
    }
    else if (jdbcUrl != null) {
      result = new DriverDataSource(jdbcUrl, driverClassName, dataSourceProperties, username, password);
    }
    else if (dataSourceJNDI != null) {
      try {
        var ic = new InitialContext();
        result = (DataSource) ic.lookup(dataSourceJNDI);
      } catch (NamingException e) {
        throw new HikariPool.PoolInitializationException(e);
      }
    }

    return result;
  }

  private static void checkDataSource(DataSource dataSource, String dataSourceName) {
    try (Connection connection = dataSource.getConnection()) {
      if (!connection.isValid(1000)) {
        throw new RuntimeException("Invalid connection to " + dataSourceName);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to check data source " + dataSourceName + ": " + e);
    }
  }
}
