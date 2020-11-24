package ru.hh.nab.starter.server.jetty;

public final class JettySettingsConstants {

  private JettySettingsConstants() {}

  public final static String JETTY = "jetty";

  public final static String ACCEPTORS = "acceptors";
  public final static String SELECTORS = "selectors";
  public final static String HOST = "host";
  public final static String PORT = "port";
  public final static String MAX_THREADS = "maxThreads";
  public final static String MIN_THREADS = "minThreads";
  public final static String QUEUE_SIZE = "queueSize";
  public final static String THREAD_POOL_IDLE_TIMEOUT_MS = "threadPoolIdleTimeoutMs";
  public final static String CONNECTION_IDLE_TIMEOUT_MS = "connectionIdleTimeoutMs";
  public final static String ACCEPT_QUEUE_SIZE = "acceptQueueSize";
  public final static String STOP_TIMEOUT_SIZE = "stopTimeoutMs";
  public final static String SECURE_PORT = "securePort";
  public final static String OUTPUT_BUFFER_SIZE = "outputBufferSize";
  public final static String REQUEST_HEADER_SIZE = "requestHeaderSize";
  public final static String RESPONSE_HEADER_SIZE = "responseHeaderSize";
  public final static String SESSION_MANAGER_ENABLED = "session-manager.enabled";

  public final static String JETTY_PORT = String.join(".", JETTY, PORT);
}
