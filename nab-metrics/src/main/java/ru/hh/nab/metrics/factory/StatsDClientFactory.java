package ru.hh.nab.metrics.factory;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClientBuilder;
import com.timgroup.statsd.StatsDClient;
import java.util.Map;
import static java.util.Optional.ofNullable;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_BUFFER_POOL_SIZE_ENV;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_BUFFER_POOL_SIZE_PROPERTY;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_HOST_ENV;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_HOST_PROPERTY;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_MAX_PACKET_SIZE_BYTES_ENV;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_MAX_PACKET_SIZE_BYTES_PROPERTY;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_PORT_ENV;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_PORT_PROPERTY;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_QUEUE_SIZE_ENV;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_QUEUE_SIZE_PROPERTY;

public class StatsDClientFactory {

  /**
   * Factory method for non-blocking statsd client with predefined sensible defaults.
   *
   * @param config Configuration properties for StatsD client
   * @return {@link NonBlockingStatsDClient}
   * @apiNote Okmeter limitations forces us to disable default telemetry and aggregation.
   * Also we disable origin detection which works only with datadog agent.
   * @see ru.hh.nab.metrics.StatsDConstants
   */
  public static StatsDClient createNonBlockingClient(Map<String, ?> config) {
    String host = ofNullable(config.get(STATSD_HOST_PROPERTY))
        .map(Object::toString)
        .or(() -> ofNullable(System.getProperty(STATSD_HOST_ENV)))
        .orElse("localhost");

    int port = ofNullable(config.get(STATSD_PORT_PROPERTY))
        .map(Object::toString)
        .or(() -> ofNullable(System.getProperty(STATSD_PORT_ENV)))
        .map(Integer::parseInt)
        .orElse(8125);

    int queueSize = ofNullable(config.get(STATSD_QUEUE_SIZE_PROPERTY))
        .map(Object::toString)
        .or(() -> ofNullable(System.getProperty(STATSD_QUEUE_SIZE_ENV)))
        .map(Integer::parseInt)
        .orElse(10_000);

    int maxPacketSizeBytes = ofNullable(config.get(STATSD_MAX_PACKET_SIZE_BYTES_PROPERTY))
        .map(Object::toString)
        .or(() -> ofNullable(System.getProperty(STATSD_MAX_PACKET_SIZE_BYTES_ENV)))
        .map(Integer::parseInt)
        .orElse(NonBlockingStatsDClient.DEFAULT_UDP_MAX_PACKET_SIZE_BYTES);

    int bufferPoolSize = ofNullable(config.get(STATSD_BUFFER_POOL_SIZE_PROPERTY))
        .map(Object::toString)
        .or(() -> ofNullable(System.getProperty(STATSD_BUFFER_POOL_SIZE_ENV)))
        .map(Integer::parseInt)
        .orElse(8);

    return new NonBlockingStatsDClientBuilder()
        .hostname(host)
        .queueSize(queueSize)
        .port(port)
        .enableAggregation(false)
        .originDetectionEnabled(false)
        .enableTelemetry(false)
        .maxPacketSizeBytes(maxPacketSizeBytes)
        .bufferPoolSize(bufferPoolSize)
        .build();
  }
}
