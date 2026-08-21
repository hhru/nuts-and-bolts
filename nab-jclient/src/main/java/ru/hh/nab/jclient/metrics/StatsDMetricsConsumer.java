package ru.hh.nab.jclient.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.metrics.MetricsConsumer;
import ru.hh.jclient.common.metrics.MetricsProvider;
import ru.hh.metrics.MetricsSender;
import ru.hh.metrics.Tag;

public class StatsDMetricsConsumer implements MetricsConsumer {

  private static final Logger log = LoggerFactory.getLogger(StatsDMetricsConsumer.class);
  private static final String NAME_KEY = "clientName";

  private final Tag nameTag;
  private final MetricsSender metricsSender;

  public StatsDMetricsConsumer(String name, MetricsSender metricsSender) {
    this.nameTag = new Tag(NAME_KEY, name);
    this.metricsSender = metricsSender;
  }

  @Override
  public void accept(MetricsProvider metricsProvider) {
    if (metricsProvider == null) {
      log.info("Metric provider contains no metrics, won't schedule anything");
      return;
    }

    metricsSender.sendPeriodically(() -> {
      metricsSender.sendGauge(
          "async.client.connection.total.count",
          metricsProvider.totalConnectionCount().get(),
          nameTag
      );
      metricsSender.sendGauge(
          "async.client.connection.active.count",
          metricsProvider.totalActiveConnectionCount().get(),
          nameTag
      );
      metricsSender.sendGauge(
          "async.client.connection.idle.count",
          metricsProvider.totalIdleConnectionCount().get(),
          nameTag
      );
      metricsSender.sendGauge(
          "async.client.usedDirectMemory",
          metricsProvider.usedDirectMemory().get(),
          nameTag
      );
      metricsSender.sendGauge(
          "async.client.usedHeapMemory",
          metricsProvider.usedHeapMemory().get(),
          nameTag
      );
      metricsSender.sendGauge(
          "async.client.numActiveSmallAllocations",
          metricsProvider.numActiveSmallAllocations().get(),
          nameTag
      );
      metricsSender.sendGauge(
          "async.client.numActiveNormalAllocations",
          metricsProvider.numActiveNormalAllocations().get(),
          nameTag
      );
      metricsSender.sendGauge(
          "async.client.numActiveHugeAllocations",
          metricsProvider.numActiveHugeAllocations().get(),
          nameTag
      );
      metricsSender.sendGauge(
          "async.client.epollTotalPendingTasks",
          metricsProvider.epollTotalPendingTasks().get(),
          nameTag
      );
      metricsSender.sendGauge(
          "async.client.nioTotalPendingTasks",
          metricsProvider.nioTotalPendingTasks().get(),
          nameTag
      );
      metricsSender.sendGauge(
          "async.client.epollPendingThreads",
          metricsProvider.epollPendingThreads().get(),
          nameTag
      );
      metricsSender.sendGauge(
          "async.client.nioPendingThreads",
          metricsProvider.nioPendingThreads().get(),
          nameTag
      );
      metricsSender.sendGauge(
          "async.client.epollMaxThreads",
          metricsProvider.epollMaxThreads().get(),
          nameTag
      );
      metricsSender.sendGauge(
          "async.client.nioMaxThreads",
          metricsProvider.nioMaxThreads().get(),
          nameTag
      );
    });

    log.info("Successfully scheduled metrics sending");
  }
}
