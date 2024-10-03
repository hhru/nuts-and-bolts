package ru.hh.nab.metrics.factory;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClientBuilder;
import com.timgroup.statsd.StatsDClient;
import ru.hh.nab.metrics.StatsDProperties;

public class StatsDClientFactory {

  /**
   * Factory method for non-blocking statsd client with predefined sensible defaults.
   *
   * @param statsDProperties Configuration properties for StatsD client
   * @return {@link NonBlockingStatsDClient}
   * @apiNote Okmeter limitations forces us to disable default telemetry and aggregation.
   * Also we disable origin detection which works only with datadog agent.
   */
  public static StatsDClient createNonBlockingClient(StatsDProperties statsDProperties) {
    return new NonBlockingStatsDClientBuilder()
        .hostname(statsDProperties.getHost())
        .queueSize(statsDProperties.getQueueSize())
        .port(statsDProperties.getPort())
        .enableAggregation(false)
        .originDetectionEnabled(false)
        .enableTelemetry(false)
        .maxPacketSizeBytes(statsDProperties.getMaxPacketSizeBytes())
        .bufferPoolSize(statsDProperties.getBufferPoolSize())
        .build();
  }
}
