package ru.hh.nab.datasource.monitoring;

public final class ConnectionPoolMetrics {
  public static final String CREATION_MS = "connection.creation_ms";
  public static final String ACQUISITION_MS = "connection.acquisition_ms";
  public static final String USAGE_MS = "connection.usage_ms";
  public static final String TOTAL_USAGE_MS = "connection.total_usage_ms";
  public static final String SAMPLED_USAGE_MS = "connection.sampled_usage_ms";

  public static final String CONNECTION_TIMEOUTS = "pool.connection_timeouts";
  public static final String ACTIVE_CONNECTIONS = "pool.active_connections";
  public static final String TOTAL_CONNECTIONS = "pool.total_connections";
  public static final String IDLE_CONNECTIONS = "pool.idle_connections";
  public static final String MAX_CONNECTIONS = "pool.max_connections";
  public static final String MIN_CONNECTIONS = "pool.min_connections";
  public static final String PENDING_THREADS = "pool.pending_threads";

  private ConnectionPoolMetrics() {
  }
}
