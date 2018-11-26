package ru.hh.nab.datasource;

public final class DataSourceSettings {
  // Connection validation timeout = connect timeout + DEFAULT_VALIDATION_TIMEOUT_INCREMENT_MS
  public static final int DEFAULT_VALIDATION_TIMEOUT_INCREMENT_MS = 100;

  public static final String JDBC_URL = "jdbcUrl";
  public static final String USER = "user";
  public static final String PASSWORD = "password";
  public static final String STATEMENT_TIMEOUT_MS = "statementTimeoutMs";

  public static final String POOL_SETTINGS_PREFIX = "pool";

  public static final String MONITORING_SEND_STATS = "monitoring.sendStats";
  public static final String MONITORING_LONG_CONNECTION_USAGE_MS = "monitoring.longConnectionUsageMs";
  public static final String MONITORING_SEND_SAMPLED_STATS = "monitoring.sendSampledStats";

  private DataSourceSettings() {
  }
}
