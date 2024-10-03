package ru.hh.nab.metrics;

public class StatsDConstants {
  /**
   * The address of a StatsD server.
   * See constructor of {@link com.timgroup.statsd.NonBlockingStatsDClient} for additional information.
   */
  public static final String STATSD_HOST_PROPERTY = "statsd.host";
  public static final String STATSD_HOST_ENV = "STATSD_HOST";
  /**
   * The port of a StatsD server.
   * See constructor of {@link com.timgroup.statsd.NonBlockingStatsDClient} for additional information.
   */
  public static final String STATSD_PORT_PROPERTY = "statsd.port";
  public static final String STATSD_PORT_ENV = "STATSD_PORT";
  /**
   * The maximum amount of unprocessed messages in the Queue.
   * See constructor of {@link com.timgroup.statsd.NonBlockingStatsDClient} for additional information.
   */
  public static final String STATSD_QUEUE_SIZE_PROPERTY = "statsd.queue.size";
  public static final String STATSD_QUEUE_SIZE_ENV = "STATSD_QUEUE_SIZE";
  /**
   * The maximum number of bytes for a message that can be sent.
   * See constructor of {@link com.timgroup.statsd.NonBlockingStatsDClient} for additional information.
   */
  public static final String STATSD_MAX_PACKET_SIZE_BYTES_PROPERTY = "statsd.maxPacketSizeBytes";
  public static final String STATSD_MAX_PACKET_SIZE_BYTES_ENV = "STATSD_MAX_PACKET_SIZE_BYTES";
  /**
   * The size for the network buffer pool.
   * See constructor of {@link com.timgroup.statsd.NonBlockingStatsDClient} for additional information.
   */
  public static final String STATSD_BUFFER_POOL_SIZE_PROPERTY = "statsd.buffer.pool.size";
  public static final String STATSD_BUFFER_POOL_SIZE_ENV = "STATSD_BUFFER_POOL_SIZE";
  /**
   * Schedule interval in seconds for {@link StatsDSender#sendPeriodically}.
   */
  public static final String STATSD_DEFAULT_PERIODIC_SEND_INTERVAL = "statsd.defaultPeriodicSendIntervalSec";
}
