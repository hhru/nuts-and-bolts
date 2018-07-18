package ru.hh.nab.datasource;

public final class DataSourceSettings {
  static final String JDBC_URL = "jdbcUrl";
  static final String USER = "user";
  static final String PASSWORD = "password";
  static final String STATEMENT_TIMEOUT_MS = "statementTimeoutMs";
  static final String C3P0_PREFIX = "c3p0";

  static final String MONITORING_SEND_STATS = "monitoring.sendStats";
  public static final String MONITORING_LONG_CONNECTION_USAGE_MS = "monitoring.longConnectionUsageMs";
  public static final String MONITORING_SEND_SAMPLED_STATS = "monitoring.sendSampledStats";

  private DataSourceSettings() {
  }
}
