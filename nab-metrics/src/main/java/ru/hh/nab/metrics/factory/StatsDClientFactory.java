package ru.hh.nab.metrics.factory;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClientBuilder;
import com.timgroup.statsd.StatsDClient;
import java.util.Properties;
import ru.hh.nab.common.properties.PropertiesUtils;

public class StatsDClientFactory {

  /**
   * The address of a StatsD server.
   * See constructor of {@link com.timgroup.statsd.NonBlockingStatsDClient} for additional information.
   */
  public static final String STATSD_HOST_PROPERTY = "statsd.host";

  /**
   * The port of a StatsD server.
   * See constructor of {@link com.timgroup.statsd.NonBlockingStatsDClient} for additional information.
   */
  public static final String STATSD_PORT_PROPERTY = "statsd.port";

  /**
   * The maximum amount of unprocessed messages in the Queue.
   * See constructor of {@link com.timgroup.statsd.NonBlockingStatsDClient} for additional information.
   */
  public static final String STATSD_QUEUE_SIZE_PROPERTY = "statsd.queue.size";

  /**
   * The maximum number of bytes for a message that can be sent.
   * See constructor of {@link com.timgroup.statsd.NonBlockingStatsDClient} for additional information.
   */
  public static final String STATSD_MAX_PACKET_SIZE_BYTES_PROPERTY = "statsd.maxPacketSizeBytes";

  /**
   * The size for the network buffer pool.
   * See constructor of {@link com.timgroup.statsd.NonBlockingStatsDClient} for additional information.
   */
  public static final String STATSD_BUFFER_POOL_SIZE_PROPERTY = "statsd.buffer.pool.size";

  public static final String DEFAULT_HOST = "localhost";
  public static final int DEFAULT_PORT = 8125;
  public static final int DEFAULT_QUEUE_SIZE = 10_000;
  public static final int DEFAULT_UDP_MAX_PACKET_SIZE = NonBlockingStatsDClient.DEFAULT_UDP_MAX_PACKET_SIZE_BYTES;
  public static final int DEFAULT_BUFFER_POOL_SIZE = 8;

  /**
   * Factory method for non-blocking statsd client with predefined sensible defaults.
   *
   * @param properties Configuration properties for StatsD client
   * @return {@link NonBlockingStatsDClient}
   * @apiNote Okmeter limitations forces us to disable default telemetry and aggregation.
   * Also we disable origin detection which works only with datadog agent.
   */
  public static StatsDClient createNonBlockingClient(Properties properties) {
    return new NonBlockingStatsDClientBuilder()
        .hostname(properties.getProperty(STATSD_HOST_PROPERTY, DEFAULT_HOST))
        .queueSize(PropertiesUtils.getInteger(properties, STATSD_QUEUE_SIZE_PROPERTY, DEFAULT_QUEUE_SIZE))
        .port(PropertiesUtils.getInteger(properties, STATSD_PORT_PROPERTY, DEFAULT_PORT))
        .enableAggregation(false)
        .originDetectionEnabled(false)
        .enableTelemetry(false)
        .maxPacketSizeBytes(PropertiesUtils.getInteger(properties, STATSD_MAX_PACKET_SIZE_BYTES_PROPERTY, DEFAULT_UDP_MAX_PACKET_SIZE))
        .bufferPoolSize(PropertiesUtils.getInteger(properties, STATSD_BUFFER_POOL_SIZE_PROPERTY, DEFAULT_BUFFER_POOL_SIZE))
        .build();
  }
}
