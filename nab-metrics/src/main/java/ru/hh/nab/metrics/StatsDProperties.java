package ru.hh.nab.metrics;

import com.timgroup.statsd.NonBlockingStatsDClient;

public class StatsDProperties {

  public static final String PREFIX = "statsd";
  public static final int DEFAULT_SEND_INTERVAL_SECONDS = 60;

  /**
   * The address of a StatsD server.
   * See constructor of {@link com.timgroup.statsd.NonBlockingStatsDClient} for additional information.
   */
  private String host = "localhost";

  /**
   * The port of a StatsD server.
   * See constructor of {@link com.timgroup.statsd.NonBlockingStatsDClient} for additional information.
   */
  private int port = 8125;

  /**
   * The maximum amount of unprocessed messages in the Queue.
   * See constructor of {@link com.timgroup.statsd.NonBlockingStatsDClient} for additional information.
   */
  private int queueSize = 10_000;

  /**
   * The maximum number of bytes for a message that can be sent.
   * See constructor of {@link com.timgroup.statsd.NonBlockingStatsDClient} for additional information.
   */
  private int maxPacketSizeBytes = NonBlockingStatsDClient.DEFAULT_UDP_MAX_PACKET_SIZE_BYTES;

  /**
   * The size for the network buffer pool.
   * See constructor of {@link com.timgroup.statsd.NonBlockingStatsDClient} for additional information.
   */
  private int bufferPoolSize = 8;

  /**
   * Schedule interval in seconds for {@link StatsDSender#sendPeriodically}.
   */
  private int defaultPeriodicSendIntervalSec = DEFAULT_SEND_INTERVAL_SECONDS;

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public int getQueueSize() {
    return queueSize;
  }

  public void setQueueSize(int queueSize) {
    this.queueSize = queueSize;
  }

  public int getMaxPacketSizeBytes() {
    return maxPacketSizeBytes;
  }

  public void setMaxPacketSizeBytes(int maxPacketSizeBytes) {
    this.maxPacketSizeBytes = maxPacketSizeBytes;
  }

  public int getBufferPoolSize() {
    return bufferPoolSize;
  }

  public void setBufferPoolSize(int bufferPoolSize) {
    this.bufferPoolSize = bufferPoolSize;
  }

  public int getDefaultPeriodicSendIntervalSec() {
    return defaultPeriodicSendIntervalSec;
  }

  public void setDefaultPeriodicSendIntervalSec(int defaultPeriodicSendIntervalSec) {
    this.defaultPeriodicSendIntervalSec = defaultPeriodicSendIntervalSec;
  }
}
