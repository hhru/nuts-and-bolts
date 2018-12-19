package ru.hh.nab.datasource;

import static java.lang.Integer.parseInt;
import static java.util.Optional.ofNullable;
import static ru.hh.nab.datasource.DataSourceSettings.DEFAULT_VALIDATION_TIMEOUT_INCREMENT_MS;
import static ru.hh.nab.datasource.DataSourceSettings.JDBC_URL;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_SEND_STATS;
import static ru.hh.nab.datasource.DataSourceSettings.PASSWORD;
import static ru.hh.nab.datasource.DataSourceSettings.POOL_SETTINGS_PREFIX;
import static ru.hh.nab.datasource.DataSourceSettings.STATEMENT_TIMEOUT_MS;
import static ru.hh.nab.datasource.DataSourceSettings.USER;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.datasource.monitoring.AbstractMetricsTrackerFactoryProvider;
import ru.hh.nab.datasource.monitoring.StatementTimeoutDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DataSourceFactory {
  private final AbstractMetricsTrackerFactoryProvider metricsTrackerFactoryProvider;

  public DataSourceFactory(AbstractMetricsTrackerFactoryProvider metricsTrackerFactoryProvider) {
    this.metricsTrackerFactoryProvider = metricsTrackerFactoryProvider;
  }

  public DataSource create(DataSourceType dataSourceType, FileSettings settings) {
    return create(dataSourceType.getName(), dataSourceType.isReadonly(), settings);
  }

  public DataSource create(String dataSourceName, boolean isReadonly, FileSettings settings) {
    return createDataSource(dataSourceName, isReadonly, settings.getSubSettings(dataSourceName));
  }

  protected DataSource createDataSource(String dataSourceName, boolean isReadonly, FileSettings dataSourceSettings) {
    DataSource underlyingDataSource = createPooledDatasource(dataSourceName, isReadonly, dataSourceSettings);

    String statementTimeoutMsVal = dataSourceSettings.getString(STATEMENT_TIMEOUT_MS);
    if (statementTimeoutMsVal != null) {
      int statementTimeoutMs = parseInt(statementTimeoutMsVal);
      if (statementTimeoutMs > 0) {
        underlyingDataSource = new StatementTimeoutDataSource(underlyingDataSource, statementTimeoutMs);
      }
    }

    checkDataSource(underlyingDataSource, dataSourceName);

    return underlyingDataSource;
  }

  private DataSource createPooledDatasource(String dataSourceName, boolean isReadonly, FileSettings dataSourceSettings) {
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

    boolean sendStats = ofNullable(dataSourceSettings.getBoolean(MONITORING_SEND_STATS)).orElse(false);
    if (sendStats && metricsTrackerFactoryProvider != null) {
      config.setMetricsTrackerFactory(metricsTrackerFactoryProvider.create(dataSourceSettings));
    }

    return new HikariDataSource(config);
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
