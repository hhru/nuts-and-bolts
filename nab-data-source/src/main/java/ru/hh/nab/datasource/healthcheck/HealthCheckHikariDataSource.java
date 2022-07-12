package ru.hh.nab.datasource.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.common.executor.ScheduledExecutor;
import static ru.hh.nab.datasource.DataSourceSettings.HEALTHCHECK_DELAY;
import ru.hh.nab.metrics.TaggedSender;

public class HealthCheckHikariDataSource extends HikariDataSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckHikariDataSource.class);
  private static final long DEFAULT_HEALTHCHECK_DELAY = 5000L;
  private static final String DATA_SOURCE_HEALTHCHECK_FAILED = "nab.db.healthcheck.failed";
  private static final String UNHEALTHY_DATA_SOURCE_CONNECTION_ATTEMPTS = "nab.db.unhealthy_datasource.connection_attempts";

  private final String dataSourceName;
  private final TaggedSender metricsSender;
  private final AsyncHealthCheckDecorator healthCheck;

  public HealthCheckHikariDataSource(HikariConfig hikariConfig, TaggedSender metricsSender) {
    super(hikariConfig);
    this.dataSourceName = hikariConfig.getPoolName();
    this.metricsSender = metricsSender;
    this.healthCheck = new AsyncHealthCheckDecorator((HealthCheckRegistry) hikariConfig.getHealthCheckRegistry(),
        Long.parseLong(hikariConfig.getHealthCheckProperties().getProperty(HEALTHCHECK_DELAY, "0")));
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
    HealthCheck.Result healthCheckResult = healthCheck.check();
    if (healthCheckResult.isHealthy()) {
      return connectionSupplier.get();
    } else {
      metricsSender.sendCount(UNHEALTHY_DATA_SOURCE_CONNECTION_ATTEMPTS, 1L);
      throw new UnhealthyDataSourceException(dataSourceName, healthCheckResult.getError());
    }
  }

  private class AsyncHealthCheckDecorator extends HealthCheck implements Runnable {

    private final HealthCheckRegistry healthCheckRegistry;
    private volatile Result result;

    private AsyncHealthCheckDecorator(HealthCheckRegistry healthCheckRegistry, long healthCheckDelayMs) {
      this.healthCheckRegistry = healthCheckRegistry;
      this.result = Result.healthy();

      ScheduledExecutorService executorService = new ScheduledExecutor();
      executorService.scheduleWithFixedDelay(this, 0L, healthCheckDelayMs > 0 ? healthCheckDelayMs : DEFAULT_HEALTHCHECK_DELAY,
          TimeUnit.MILLISECONDS);
    }

    @Override
    public Result check() {
      return result;
    }

    @Override
    public void run() {
      result = healthCheckRegistry.runHealthChecks().values().stream()
          .filter(result -> !result.isHealthy())
          .findAny()
          .orElseGet(Result::healthy);

      if (result.isHealthy()) {
        metricsSender.sendGauge(DATA_SOURCE_HEALTHCHECK_FAILED, 0L);
        LOGGER.debug("DataSource {} is healthy", dataSourceName);
      } else {
        metricsSender.sendGauge(DATA_SOURCE_HEALTHCHECK_FAILED, 1L);
        LOGGER.error("DataSource {} is unhealthy: {}", dataSourceName, result.getMessage(), result.getError());
      }
    }
  }

  @FunctionalInterface
  private interface ConnectionSupplier {
    Connection get() throws SQLException;
  }
}
