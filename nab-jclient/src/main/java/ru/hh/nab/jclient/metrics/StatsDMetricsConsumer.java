package ru.hh.nab.jclient.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.metrics.MetricsConsumer;
import ru.hh.jclient.common.metrics.MetricsProvider;
import ru.hh.metrics.StatsDSender;
import ru.hh.metrics.Tag;

public class StatsDMetricsConsumer implements MetricsConsumer {

  private static final Logger log = LoggerFactory.getLogger(StatsDMetricsConsumer.class);
  private static final String NAME_KEY = "clientName";

  private final Tag nameTag;
  private final StatsDSender statsDSender;
  private final int sendIntervalInSeconds;

  public StatsDMetricsConsumer(String name, StatsDSender statsDSender, int sendIntervalInSeconds) {
    this.nameTag = new Tag(NAME_KEY, name);
    this.statsDSender = statsDSender;
    this.sendIntervalInSeconds = sendIntervalInSeconds;
  }

  @Override
  public void accept(MetricsProvider metricsProvider) {
    if (metricsProvider == null) {
      log.info("Metric provider contains no metrics, won't schedule anything");
      return;
    }

    statsDSender.sendPeriodically(() -> {
      statsDSender.sendGauge(
          "async.client.connection.total.count",
          metricsProvider.totalConnectionCount().get(),
          nameTag
      );
      statsDSender.sendGauge(
          "async.client.connection.active.count",
          metricsProvider.totalActiveConnectionCount().get(),
          nameTag
      );
      statsDSender.sendGauge(
          "async.client.connection.idle.count",
          metricsProvider.totalIdleConnectionCount().get(),
          nameTag
      );
      statsDSender.sendGauge(
          "async.client.usedDirectMemory",
          metricsProvider.usedDirectMemory().get(),
          nameTag
      );
      statsDSender.sendGauge(
          "async.client.usedHeapMemory",
          metricsProvider.usedHeapMemory().get(),
          nameTag
      );
      statsDSender.sendGauge(
          "async.client.numActiveSmallAllocations",
          metricsProvider.numActiveSmallAllocations().get(),
          nameTag
      );
      statsDSender.sendGauge(
          "async.client.numActiveNormalAllocations",
          metricsProvider.numActiveNormalAllocations().get(),
          nameTag
      );
      statsDSender.sendGauge(
          "async.client.numActiveHugeAllocations",
          metricsProvider.numActiveHugeAllocations().get(),
          nameTag
      );
      statsDSender.sendGauge(
          "async.client.epollTotalPendingTasks",
          metricsProvider.epollTotalPendingTasks().get(),
          nameTag
      );
      statsDSender.sendGauge(
          "async.client.nioTotalPendingTasks",
          metricsProvider.nioTotalPendingTasks().get(),
          nameTag
      );
      statsDSender.sendGauge(
          "async.client.epollPendingThreads",
          metricsProvider.epollPendingThreads().get(),
          nameTag
      );
      statsDSender.sendGauge(
          "async.client.nioPendingThreads",
          metricsProvider.nioPendingThreads().get(),
          nameTag
      );
      statsDSender.sendGauge(
          "async.client.epollMaxThreads",
          metricsProvider.epollMaxThreads().get(),
          nameTag
      );
      statsDSender.sendGauge(
          "async.client.nioMaxThreads",
          metricsProvider.nioMaxThreads().get(),
          nameTag
      );
    }, sendIntervalInSeconds);

    log.info("Successfully scheduled metrics sending");
  }
}
