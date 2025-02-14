package ru.hh.nab.datasource;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import com.zaxxer.hikari.util.DriverDataSource;
import com.zaxxer.hikari.util.PropertyElf;
import static com.zaxxer.hikari.util.UtilityElf.createInstance;
import jakarta.annotation.Nullable;
import static java.lang.Integer.parseInt;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.common.properties.PropertiesUtils;
import ru.hh.nab.common.servlet.UriComponent;
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
import ru.hh.nab.datasource.routing.DatabaseSwitcher;

public class DataSourceFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceFactory.class);
  private static final int HIKARI_MIN_VALIDATION_TIMEOUT_MS = 250;

  private final MetricsTrackerFactoryProvider<?> metricsTrackerFactoryProvider;
  @Nullable
  private final HealthCheckHikariDataSourceFactory healthCheckHikariDataSourceFactory;
  @Nullable
  private final OpenTelemetryJdbcExtension openTelemetryJdbcExtension;
  @Nullable
  private final DatabaseSwitcher databaseSwitcher;

  public DataSourceFactory(
      MetricsTrackerFactoryProvider<?> metricsTrackerFactoryProvider,
      @Nullable HealthCheckHikariDataSourceFactory healthCheckHikariDataSourceFactory,
      @Nullable OpenTelemetryJdbcExtension openTelemetryJdbcExtension
  ) {
    this(metricsTrackerFactoryProvider, healthCheckHikariDataSourceFactory, openTelemetryJdbcExtension, null);
  }

  public DataSourceFactory(
      MetricsTrackerFactoryProvider<?> metricsTrackerFactoryProvider,
      @Nullable HealthCheckHikariDataSourceFactory healthCheckHikariDataSourceFactory,
      @Nullable OpenTelemetryJdbcExtension openTelemetryJdbcExtension,
      @Nullable DatabaseSwitcher databaseSwitcher
  ) {
    this.metricsTrackerFactoryProvider = metricsTrackerFactoryProvider;
    this.healthCheckHikariDataSourceFactory = healthCheckHikariDataSourceFactory;
    this.openTelemetryJdbcExtension = openTelemetryJdbcExtension;
    this.databaseSwitcher = databaseSwitcher;
  }

  public DataSource create(String databaseName, String dataSourceType, boolean isReadonly, Properties properties) {
    if (databaseSwitcher == null) {
      throw new IllegalStateException("If your application needs to work with multiple databases, you should create DatabaseSwitcher bean");
    } else {
      String dataSourceName = databaseSwitcher.createDataSourceName(databaseName, dataSourceType);
      return create(dataSourceName, isReadonly, properties);
    }
  }

  public DataSource create(String dataSourceName, boolean isReadonly, Properties properties) {
    return createDataSource(dataSourceName, isReadonly, PropertiesUtils.getSubProperties(properties, dataSourceName));
  }

  public DataSource create(HikariConfig hikariConfig, Properties dataSourceProperties, boolean isReadonly) {
    String dataSourceName = hikariConfig.getPoolName();
    try {
      boolean sendStats = PropertiesUtils.getBoolean(dataSourceProperties, MONITORING_SEND_STATS, false);
      if (sendStats && metricsTrackerFactoryProvider != null) {
        hikariConfig.setMetricsTrackerFactory(metricsTrackerFactoryProvider.create(dataSourceProperties));
      }

      Properties healthCheckProperties = PropertiesUtils.getSubProperties(dataSourceProperties, HEALTHCHECK_SETTINGS_PREFIX);
      boolean healthCheckEnabled = PropertiesUtils.getBoolean(healthCheckProperties, HEALTHCHECK_ENABLED, false);
      String secondaryDataSource = dataSourceProperties.getProperty(ROUTING_SECONDARY_DATASOURCE);
      if (!healthCheckEnabled && secondaryDataSource != null) {
        throw new RuntimeException(String.format(
            "Exception during %s datasource initialization: if %s is configured, healthcheck should be enabled. " +
                "To prevent misconfiguration application startup will be aborted.", hikariConfig.getPoolName(), ROUTING_SECONDARY_DATASOURCE
        ));
      }

      DataSource originalDataSource = Optional.ofNullable(hikariConfig.getDataSource()).orElseGet(() -> createOriginalDataSource(hikariConfig));
      DataSource namedDataSource = new NamedDataSource(dataSourceName, originalDataSource);
      DataSource telemetryDataSource = openTelemetryJdbcExtension == null ? namedDataSource : openTelemetryJdbcExtension.wrap(namedDataSource);
      hikariConfig.setDataSource(telemetryDataSource);

      DataSource hikariDataSource;
      if (healthCheckHikariDataSourceFactory != null && healthCheckEnabled) {
        hikariConfig.setHealthCheckRegistry(new HealthCheckRegistry());
        hikariConfig.setHealthCheckProperties(healthCheckProperties);
        hikariDataSource = healthCheckHikariDataSourceFactory.create(hikariConfig);
      } else {
        hikariDataSource = new HikariDataSource(hikariConfig);
      }

      String statementTimeoutMsVal = dataSourceProperties.getProperty(STATEMENT_TIMEOUT_MS);
      if (statementTimeoutMsVal != null) {
        int statementTimeoutMs = parseInt(statementTimeoutMsVal);
        if (statementTimeoutMs > 0) {
          hikariDataSource = new StatementTimeoutDataSource(hikariDataSource, statementTimeoutMs);
        }
      }

      checkDataSource(hikariDataSource, dataSourceName);
      DataSourcePropertiesStorage.registerPropertiesFor(
          hikariConfig.getPoolName(),
          new DataSourcePropertiesStorage.DataSourceProperties(!isReadonly, secondaryDataSource)
      );

      return hikariDataSource;
    } catch (RuntimeException e) {
      RuntimeException dataSourceCreationException = e;
      Matcher matcher = Pattern.compile("jdbc:(.*)").matcher(hikariConfig.getJdbcUrl());
      if (matcher.find()) {
        URI uri = URI.create(matcher.group(1));
        Map<String, List<String>> queryParams = UriComponent.decodeQuery(uri.getQuery(), false, false);
        String host = uri.getHost();
        String user = Optional
            .ofNullable(queryParams.get("user"))
            .map(Collection::stream)
            .flatMap(Stream::findFirst)
            .or(() -> Optional.ofNullable(hikariConfig.getUsername()))
            .orElse("unknown");

        dataSourceCreationException = new RuntimeException("%s. %s@%s (%s)".formatted(e.getMessage(), user, host, dataSourceName));
        dataSourceCreationException.addSuppressed(e);
      }
      LOGGER.error(dataSourceCreationException.getMessage(), dataSourceCreationException);
      throw dataSourceCreationException;
    }
  }

  protected DataSource createDataSource(String dataSourceName, boolean isReadonly, Properties dataSourceProperties) {
    HikariConfig hikariConfig = createBaseHikariConfig(dataSourceName, dataSourceProperties);
    return create(hikariConfig, dataSourceProperties, isReadonly);
  }

  private static HikariConfig createBaseHikariConfig(String dataSourceName, Properties dataSourceProperties) {
    Properties poolProperties = PropertiesUtils.getSubProperties(dataSourceProperties, POOL_SETTINGS_PREFIX);
    if (poolProperties.isEmpty()) {
      throw new RuntimeException(String.format(
        "Exception during %1$s pooled datasource initialization: could not find %1$s.%2$s settings in config file. " +
        "To prevent misconfiguration application startup will be aborted.",
        dataSourceName, POOL_SETTINGS_PREFIX
      ));
    }

    HikariConfig config = new HikariConfig(poolProperties);
    config.setJdbcUrl(dataSourceProperties.getProperty(JDBC_URL));
    config.setUsername(dataSourceProperties.getProperty(USER));
    config.setPassword(dataSourceProperties.getProperty(PASSWORD));
    config.setPoolName(dataSourceName);
    config.setReadOnly(false);

    long validationTimeoutMs = Math.round(config.getConnectionTimeout() * DEFAULT_VALIDATION_TIMEOUT_RATIO);
    config.setValidationTimeout(Math.max(validationTimeoutMs, HIKARI_MIN_VALIDATION_TIMEOUT_MS));

    return config;
  }

  DataSource createOriginalDataSource(HikariConfig hikariConfig) {
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
    } else if (jdbcUrl != null) {
      result = new DriverDataSource(jdbcUrl, driverClassName, dataSourceProperties, username, password);
    } else if (dataSourceJNDI != null) {
      try {
        var ic = new InitialContext();
        result = (DataSource) ic.lookup(dataSourceJNDI);
      } catch (NamingException e) {
        throw new HikariPool.PoolInitializationException(e);
      }
    }

    return result;
  }

  void checkDataSource(DataSource dataSource, String dataSourceName) {
    try (Connection connection = dataSource.getConnection()) {
      if (!connection.isValid(1000)) {
        throw new RuntimeException("Invalid connection to " + dataSourceName);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to check data source " + dataSourceName + ": " + e);
    }
  }
}
