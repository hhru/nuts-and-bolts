package ru.hh.nab.datasource.monitoring;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntConsumer;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.metrics.Counters;
import ru.hh.metrics.Histogram;
import ru.hh.metrics.StatsDSender;
import ru.hh.metrics.Tag;
import ru.hh.nab.common.mdc.MDC;
import ru.hh.nab.common.properties.FileSettings;
import static java.util.Optional.ofNullable;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_LONG_CONNECTION_USAGE_MS;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_SEND_SAMPLED_STATS;

public class MonitoringDataSourceFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringDataSourceFactory.class);

  private final String serviceName;
  private final StatsDSender statsDSender;

  public MonitoringDataSourceFactory(String serviceName, StatsDSender statsDSender) {
    this.serviceName = serviceName;
    this.statsDSender = statsDSender;
  }

  public MonitoringDataSource create(FileSettings dataSourceSettings, DataSource underlyingDataSource, String dataSourceName) {
    int longUsageConnectionMs = dataSourceSettings.getInteger(MONITORING_LONG_CONNECTION_USAGE_MS);
    boolean sendSampledStats = ofNullable(dataSourceSettings.getBoolean(MONITORING_SEND_SAMPLED_STATS)).orElse(Boolean.FALSE);
    return new MonitoringDataSource(
        underlyingDataSource,
        dataSourceName,
        createConnectionGetMsConsumer(dataSourceName),
        createConnectionUsageMsConsumer(dataSourceName, longUsageConnectionMs, sendSampledStats)
    );
  }

  private IntConsumer createConnectionGetMsConsumer(String dataSourceName) {
    Histogram histogram = new Histogram(2000);
    statsDSender.sendPercentilesPeriodically(getMetricName(dataSourceName, ConnectionMetrics.GET_MS), histogram, 50, 99, 100);
    return histogram::save;
  }

  private IntConsumer createConnectionUsageMsConsumer(String dataSourceName, int longConnectionUsageMs, boolean sendSampledStats) {

    Counters totalUsageCounter = new Counters(500);
    statsDSender.sendCountersPeriodically(getMetricName(dataSourceName, ConnectionMetrics.TOTAL_USAGE_MS), totalUsageCounter);

    Histogram histogram = new Histogram(2000);
    statsDSender.sendPercentilesPeriodically(getMetricName(dataSourceName, ConnectionMetrics.USAGE_MS), histogram, 50, 97, 99, 100);

    CompressedStackFactory compressedStackFactory;
    Counters sampledUsageCounters;
    if (sendSampledStats) {
      compressedStackFactory = new CompressedStackFactory(
          "MonitoringConnection", "close",
          "org.glassfish.jersey.servlet.ServletContainer", "service",
          new String[]{"ru.hh."},
          new String[]{"DataSourceContext", "ExecuteOnDataSource", "TransactionManager"}
      );
      sampledUsageCounters = new Counters(2000);
      statsDSender.sendCountersPeriodically(getMetricName(dataSourceName, ConnectionMetrics.SAMPLED_USAGE_MS), sampledUsageCounters);

    } else {
      sampledUsageCounters = null;
      compressedStackFactory = null;
    }

    return (usageMs) -> {

      if (usageMs > longConnectionUsageMs) {
        String message = String.format(
            "%s connection was used for more than %d ms (%d ms), not fatal, but should be fixed",
            dataSourceName, longConnectionUsageMs, usageMs);
        LOGGER.error(message, new RuntimeException(dataSourceName + " connection usage duration exceeded"));
      }

      histogram.save(usageMs);

      String controller = MDC.getController().orElse("unknown");
      Tag controllerTag = new Tag("controller", controller);
      totalUsageCounter.add(usageMs, controllerTag);

      if (sendSampledStats && ThreadLocalRandom.current().nextInt(100) == 0) {
        String compressedStack = compressedStackFactory.create();
        sampledUsageCounters.add(usageMs, new Tag("stack", compressedStack));
      }
    };
  }

  private String getMetricName(String dataSourceName, String shortName) {
    return serviceName + '.' + dataSourceName + '.' + shortName;
  }
}
