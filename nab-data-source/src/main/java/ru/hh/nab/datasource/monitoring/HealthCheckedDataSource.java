package ru.hh.nab.datasource.monitoring;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.common.executor.ScheduledExecutor;

public class HealthCheckedDataSource extends DelegatingDataSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckedDataSource.class);

  private final String dataSourceName;
  private final AsyncHealthCheckDecorator healthCheck;

  public HealthCheckedDataSource(DataSource delegate, String dataSourceName,
                                 HealthCheckRegistry healthCheckRegistry, long healthCheckDelayMs) {
    super(delegate);
    this.dataSourceName = dataSourceName;
    this.healthCheck = new AsyncHealthCheckDecorator(healthCheckRegistry, healthCheckDelayMs);
  }

  @Override
  public Connection getConnection() throws SQLException {
    HealthCheck.Result healthCheckResult = healthCheck.check();
    if (healthCheckResult.isHealthy()) {
      return super.getConnection();
    } else {
      throw new UnhealthyDataSourceException(dataSourceName, healthCheckResult.getError());
    }
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    HealthCheck.Result healthCheckResult = healthCheck.check();
    if (healthCheckResult.isHealthy()) {
      return super.getConnection(username, password);
    } else {
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
        LOGGER.debug("DataSource {} is healthy", dataSourceName);
      } else {
        LOGGER.error("DataSource {} is unhealthy: {}", dataSourceName, result.getMessage(), result.getError());
      }
    }
  }
}
