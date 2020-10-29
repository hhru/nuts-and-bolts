package ru.hh.nab.datasource.monitoring;

public final class ConnectionPoolMetrics {
  public static final String CREATION_MS = "jdbc_pool.connection.creation_ms";
  public static final String ACQUISITION_MS = "jdbc_pool.connection.acquisition_ms";
  public static final String USAGE_MS = "jdbc_pool.connection.usage_ms";
  public static final String TOTAL_USAGE_MS = "jdbc_pool.connection.total_usage_ms";
  public static final String SAMPLED_USAGE_MS = "jdbc_pool.connection.sampled_usage_ms";

  public static final String CONNECTION_TIMEOUTS = "jdbc_pool.connection_timeouts";
  public static final String ACTIVE_CONNECTIONS = "jdbc_pool.active_connections";
  public static final String TOTAL_CONNECTIONS = "jdbc_pool.total_connections";
  public static final String IDLE_CONNECTIONS = "jdbc_pool.idle_connections";
  public static final String MAX_CONNECTIONS = "jdbc_pool.max_connections";
  public static final String MIN_CONNECTIONS = "jdbc_pool.min_connections";
  public static final String PENDING_THREADS = "jdbc_pool.pending_threads";


  private ConnectionPoolMetrics() {
  }
}
