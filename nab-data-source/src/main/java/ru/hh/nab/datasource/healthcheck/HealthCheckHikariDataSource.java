package ru.hh.nab.datasource.healthcheck;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import static java.util.Optional.ofNullable;
import static ru.hh.nab.datasource.DataSourceSettings.HEALTHCHECK_DELAY;
import ru.hh.nab.metrics.TaggedSender;

public class HealthCheckHikariDataSource extends HikariDataSource implements HealthCheckDataSource {

  private static final long DEFAULT_HEALTHCHECK_DELAY = 5000L;

  private final String dataSourceName;
  private final AsyncHealthCheck healthCheck;

  public HealthCheckHikariDataSource(HikariConfig hikariConfig, TaggedSender metricsSender) {
    super(hikariConfig);
    this.dataSourceName = hikariConfig.getPoolName();
    Long healthCheckDelayMs = ofNullable(hikariConfig.getHealthCheckProperties().getProperty(HEALTHCHECK_DELAY))
        .map(Long::parseLong)
        .filter(delay -> delay > 0)
        .orElse(DEFAULT_HEALTHCHECK_DELAY);
    this.healthCheck = new AsyncHealthCheck(
        dataSourceName,
        (HealthCheckRegistry) hikariConfig.getHealthCheckRegistry(),
        metricsSender,
        healthCheckDelayMs
    );
  }

  @Override
  public String getDataSourceName() {
    return dataSourceName;
  }

  @Override
  public HealthCheck getHealthCheck() {
    return healthCheck;
  }

  @Override
  public Connection getConnection() throws SQLException {
    return getConnection(super::getConnection);
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return getConnection(() -> super.getConnection(username, password));
  }

  private Connection getConnection(ConnectionSupplier connectionSupplier) throws SQLException {
    com.codahale.metrics.health.HealthCheck.Result healthCheckResult = healthCheck.check();
    if (healthCheckResult.isHealthy()) {
      return connectionSupplier.get();
    } else {
      throw new UnhealthyDataSourceException(dataSourceName, healthCheckResult.getError());
    }
  }

  @FunctionalInterface
  private interface ConnectionSupplier {
    Connection get() throws SQLException;
  }
}
