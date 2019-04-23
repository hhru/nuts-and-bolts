package ru.hh.nab.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import static java.lang.Integer.parseInt;
import java.sql.Connection;
import java.sql.SQLException;
import static java.util.Optional.ofNullable;
import java.util.Properties;
import javax.sql.DataSource;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.datasource.DataSourceSettings.DEFAULT_VALIDATION_TIMEOUT_INCREMENT_MS;
import static ru.hh.nab.datasource.DataSourceSettings.JDBC_URL;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_SEND_STATS;
import static ru.hh.nab.datasource.DataSourceSettings.PASSWORD;
import static ru.hh.nab.datasource.DataSourceSettings.POOL_SETTINGS_PREFIX;
import static ru.hh.nab.datasource.DataSourceSettings.STATEMENT_TIMEOUT_MS;
import static ru.hh.nab.datasource.DataSourceSettings.USER;
import ru.hh.nab.datasource.monitoring.MetricsTrackerFactoryProvider;
import ru.hh.nab.datasource.monitoring.StatementTimeoutDataSource;

public class DataSourceFactory {
  private final MetricsTrackerFactoryProvider metricsTrackerFactoryProvider;

  public DataSourceFactory(MetricsTrackerFactoryProvider metricsTrackerFactoryProvider) {
    this.metricsTrackerFactoryProvider = metricsTrackerFactoryProvider;
  }

  public DataSource create(String dataSourceName, boolean isReadonly, FileSettings settings) {
    return createDataSource(dataSourceName, isReadonly, settings.getSubSettings(dataSourceName));
  }

  public DataSource create(HikariConfig hikariConfig, FileSettings dataSourceSettings) {
    boolean sendStats = ofNullable(dataSourceSettings.getBoolean(MONITORING_SEND_STATS)).orElse(false);
    if (sendStats && metricsTrackerFactoryProvider != null) {
      hikariConfig.setMetricsTrackerFactory(metricsTrackerFactoryProvider.create(dataSourceSettings));
    }

    DataSource hikariDataSource = new HikariDataSource(hikariConfig);

    String statementTimeoutMsVal = dataSourceSettings.getString(STATEMENT_TIMEOUT_MS);
    if (statementTimeoutMsVal != null) {
      int statementTimeoutMs = parseInt(statementTimeoutMsVal);
      if (statementTimeoutMs > 0) {
        hikariDataSource = new StatementTimeoutDataSource(hikariDataSource, statementTimeoutMs);
      }
    }

    checkDataSource(hikariDataSource, hikariConfig.getPoolName());
    DataSourceType.registerPropertiesFor(hikariConfig);
    return hikariDataSource;
  }

  protected DataSource createDataSource(String dataSourceName, boolean isReadonly, FileSettings dataSourceSettings) {
    HikariConfig hikariConfig = createBaseHikariConfig(dataSourceName, isReadonly, dataSourceSettings);
    return create(hikariConfig, dataSourceSettings);
  }

  private static HikariConfig createBaseHikariConfig(String dataSourceName, boolean isReadonly, FileSettings dataSourceSettings) {
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
    config.setReadOnly(isReadonly);
    config.setValidationTimeout(config.getConnectionTimeout() + DEFAULT_VALIDATION_TIMEOUT_INCREMENT_MS);
    return config;
  }

  private static void checkDataSource(DataSource dataSource, String dataSourceName) {
    try (Connection connection = dataSource.getConnection()) {
      if (!connection.isValid(1000)) {
        throw new RuntimeException("Invalid connection to " + dataSourceName);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to check data source " + dataSourceName + ": " + e.toString());
    }
  }
}
