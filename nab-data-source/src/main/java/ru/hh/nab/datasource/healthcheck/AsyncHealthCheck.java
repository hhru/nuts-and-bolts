package ru.hh.nab.datasource.healthcheck;

import com.codahale.metrics.health.HealthCheckRegistry;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.common.executor.ScheduledExecutor;
import ru.hh.nab.jdbc.healthcheck.HealthCheck;
import ru.hh.nab.jdbc.healthcheck.HealthCheckResult;
import ru.hh.nab.metrics.TaggedSender;

public class AsyncHealthCheck extends com.codahale.metrics.health.HealthCheck implements HealthCheck, Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(AsyncHealthCheck.class);
  private static final String FAILED_HEALTHCHECK_METRIC_NAME = "nab.db.healthcheck.failed";

  private final String dataSourceName;
  private final HealthCheckRegistry healthCheckRegistry;
  private final TaggedSender metricsSender;
  private final AtomicReference<Result> result = new AtomicReference<>(Result.healthy());

  public AsyncHealthCheck(String dataSourceName, HealthCheckRegistry healthCheckRegistry, TaggedSender metricsSender, long healthCheckDelayMs) {
    this.dataSourceName = dataSourceName;
    this.healthCheckRegistry = healthCheckRegistry;
    this.metricsSender = metricsSender;

    ScheduledExecutorService executorService = new ScheduledExecutor();
    executorService.scheduleWithFixedDelay(this, 0L, healthCheckDelayMs, TimeUnit.MILLISECONDS);
  }

  @Override
  public Result check() {
    return result.get();
  }

  @Override
  public HealthCheckResult getCheckResult() {
    return new HealthCheckResultImpl(result.get().isHealthy());
  }

  @Override
  public void run() {
    Result newResult = healthCheckRegistry
        .runHealthChecks()
        .values()
        .stream()
        .filter(result -> !result.isHealthy())
        .findAny()
        .orElseGet(Result::healthy);
    result.set(newResult);

    if (newResult.isHealthy()) {
      metricsSender.sendGauge(FAILED_HEALTHCHECK_METRIC_NAME, 0L);
      LOGGER.debug("DataSource {} is healthy", dataSourceName);
    } else {
      metricsSender.sendGauge(FAILED_HEALTHCHECK_METRIC_NAME, 1L);
      LOGGER.error("DataSource {} is unhealthy: {}", dataSourceName, newResult.getMessage(), newResult.getError());
    }
  }
}
