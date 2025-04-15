package ru.hh.nab.jclient.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.metrics.MetricsConsumer;
import ru.hh.jclient.common.metrics.MetricsProvider;
import ru.hh.nab.metrics.StatsDSender;

public class StatsDMetricsConsumer implements MetricsConsumer {

  private static final Logger log = LoggerFactory.getLogger(StatsDMetricsConsumer.class);
  private static final String NAME_KEY = "clientName";

  private final String nameTag;
  private final StatsDSender statsDSender;
  private final int sendIntervalInSeconds;

  public StatsDMetricsConsumer(String name, StatsDSender statsDSender, int sendIntervalInSeconds) {
    this.nameTag = buildNameTag(name);
    this.statsDSender = statsDSender;
    this.sendIntervalInSeconds = sendIntervalInSeconds;
  }

  private static String buildNameTag(String name) {
    return NAME_KEY + "_is_" + name.replace('.', '-');
  }

  @Override
  public void accept(MetricsProvider metricsProvider) {
    if (metricsProvider == null) {
      log.info("Metric provider contains no metrics, won't schedule anything");
      return;
    }

    statsDSender.sendPeriodically(() -> {
      statsDSender.sendGauge(
          getFullMetricName("async.client.connection.total.count", nameTag),
          metricsProvider.totalConnectionCount().get()
      );
      statsDSender.sendGauge(
          getFullMetricName("async.client.connection.active.count", nameTag),
          metricsProvider.totalActiveConnectionCount().get()
      );
      statsDSender.sendGauge(
          getFullMetricName("async.client.connection.idle.count", nameTag),
          metricsProvider.totalIdleConnectionCount().get()
      );
      statsDSender.sendGauge(
          getFullMetricName("async.client.usedDirectMemory", nameTag),
          metricsProvider.usedDirectMemory().get()
      );
      statsDSender.sendGauge(
          getFullMetricName("async.client.usedHeapMemory", nameTag),
          metricsProvider.usedHeapMemory().get()
      );
      statsDSender.sendGauge(
          getFullMetricName("async.client.numActiveTinyAllocations", nameTag),
          metricsProvider.numActiveTinyAllocations().get()
      );
      statsDSender.sendGauge(
          getFullMetricName("async.client.numActiveSmallAllocations", nameTag),
          metricsProvider.numActiveSmallAllocations().get()
      );
      statsDSender.sendGauge(
          getFullMetricName("async.client.numActiveNormalAllocations", nameTag),
          metricsProvider.numActiveNormalAllocations().get()
      );
      statsDSender.sendGauge(
          getFullMetricName("async.client.numActiveHugeAllocations", nameTag),
          metricsProvider.numActiveHugeAllocations().get()
      );
    }, sendIntervalInSeconds);

    log.info("Successfully scheduled metrics sending");
  }

  private static String getFullMetricName(String metricName, String... tags) {
    if (tags == null) {
      return metricName;
    }
    return String.join(".", metricName, String.join(".", tags));
  }
}
