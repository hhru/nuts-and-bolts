package ru.hh.nab.datasource;

public final class DataSourceSettings {
  public static final String DATASOURCE_NAME_FORMAT = "%s.%s";

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

  public static final String HEALTHCHECK_SETTINGS_PREFIX = "healthcheck";
  public static final String HEALTHCHECK_ENABLED = "enabled";
  public static final String HEALTHCHECK_DELAY = "delayMs";

  public static final String ROUTING_SECONDARY_DATASOURCE = "routing.failedHealthcheck.secondaryDataSource";

  private DataSourceSettings() {
  }
}
