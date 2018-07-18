package ru.hh.nab.datasource.monitoring;

final class ConnectionMetrics {
  static final String GET_MS = "connection.get_ms";
  static final String USAGE_MS = "connection.usage_ms";
  static final String TOTAL_USAGE_MS = "connection.total_usage_ms";
  static final String SAMPLED_USAGE_MS = "connection.sampled_usage_ms";

  private ConnectionMetrics() {
  }
}
