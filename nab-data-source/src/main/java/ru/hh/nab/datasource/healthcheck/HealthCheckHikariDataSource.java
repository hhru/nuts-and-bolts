package ru.hh.nab.datasource.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import static java.util.Optional.ofNullable;
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
  private static final String FAILED_HEALTHCHECK_METRIC_NAME = "nab.db.healthcheck.failed";

  private final String dataSourceName;
  private final AsyncHealthCheckDecorator healthCheck;

  public HealthCheckHikariDataSource(HikariConfig hikariConfig, TaggedSender metricsSender) {
    super(hikariConfig);
    this.dataSourceName = hikariConfig.getPoolName();
    Long healthCheckDelayMs = ofNullable(hikariConfig.getHealthCheckProperties().getProperty(HEALTHCHECK_DELAY))
        .map(Long::parseLong)
        .filter(delay -> delay > 0)
        .orElse(DEFAULT_HEALTHCHECK_DELAY);
    this.healthCheck = new AsyncHealthCheckDecorator((HealthCheckRegistry) hikariConfig.getHealthCheckRegistry(), metricsSender, healthCheckDelayMs);
  }

  public AsyncHealthCheckDecorator getHealthCheck() {
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
    HealthCheck.Result healthCheckResult = healthCheck.check();
    if (healthCheckResult.isHealthy()) {
      return connectionSupplier.get();
    } else {
      throw new UnhealthyDataSourceException(dataSourceName, healthCheckResult.getError());
    }
  }

  public class AsyncHealthCheckDecorator extends HealthCheck implements Runnable {

    private final HealthCheckRegistry healthCheckRegistry;
    private final TaggedSender metricsSender;
    private volatile Result result;

    private AsyncHealthCheckDecorator(HealthCheckRegistry healthCheckRegistry, TaggedSender metricsSender, long healthCheckDelayMs) {
      this.healthCheckRegistry = healthCheckRegistry;
      this.metricsSender = metricsSender;
      this.result = Result.healthy();

      ScheduledExecutorService executorService = new ScheduledExecutor();
      executorService.scheduleWithFixedDelay(this, 0L, healthCheckDelayMs, TimeUnit.MILLISECONDS);
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
        metricsSender.sendGauge(FAILED_HEALTHCHECK_METRIC_NAME, 0L);
        LOGGER.debug("DataSource {} is healthy", dataSourceName);
      } else {
        metricsSender.sendGauge(FAILED_HEALTHCHECK_METRIC_NAME, 1L);
        LOGGER.error("DataSource {} is unhealthy: {}", dataSourceName, result.getMessage(), result.getError());
      }
    }
  }

  @FunctionalInterface
  private interface ConnectionSupplier {
    Connection get() throws SQLException;
  }
}
