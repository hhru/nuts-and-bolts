package ru.hh.nab.datasource;

public final class DataSourceSettings {
  // validationTimeout = connectionTimeout * DEFAULT_VALIDATION_TIMEOUT_RATIO
  public static final double DEFAULT_VALIDATION_TIMEOUT_RATIO = 0.8;

  public static final String JDBC_URL = "jdbcUrl";
  public static final String USER = "user";
  public static final String PASSWORD = "password";
  public static final String STATEMENT_TIMEOUT_MS = "statementTimeoutMs";

  public static final String POOL_SETTINGS_PREFIX = "pool";

  public static final String MONITORING_SEND_STATS = "monitoring.sendStats";
  public static final String MONITORING_LONG_CONNECTION_USAGE_MS = "monitoring.longConnectionUsageMs";
  public static final String MONITORING_SEND_SAMPLED_STATS = "monitoring.sendSampledStats";

  public static final String HEALTH_CHECK_DELAY = "healthcheck.delay";

  private DataSourceSettings() {
  }
}
