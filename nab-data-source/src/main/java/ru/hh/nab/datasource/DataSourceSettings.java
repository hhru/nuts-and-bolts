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
  public static final String MONITORING_SAMPLE_POOL_USAGE_STATS = "monitoring.samplePoolUsageStats";

  public static final String MONITORING_CREATION_HISTOGRAM_SIZE = "monitoring.creation.histogramSize";
  public static final String MONITORING_CREATION_HISTOGRAM_COMPACTION_RATIO = "monitoring.creation.histogramCompactionRatio";
  public static final String MONITORING_ACQUISITION_HISTOGRAM_SIZE = "monitoring.acquisition.histogramSize";
  public static final String MONITORING_ACQUISITION_HISTOGRAM_COMPACTION_RATIO = "monitoring.acquisition.histogramCompactionRatio";
  public static final String MONITORING_USAGE_HISTOGRAM_SIZE = "monitoring.usage.histogramSize";
  public static final String MONITORING_USAGE_HISTOGRAM_COMPACTION_RATIO = "monitoring.usage.histogramCompactionRatio";
  public static final String MONITORING_SAMPLED_USAGE_MAX_NUM_OF_COUNTERS = "monitoring.sampledUsage.maxNumOfCounters";
  public static final String MONITORING_TOTAL_USAGE_MAX_NUM_OF_COUNTERS = "monitoring.totalUsage.maxNumOfCounters";
  public static final String MONITORING_CONNECTION_TIMEOUT_MAX_NUM_OF_COUNTERS = "monitoring.connectionTimeout.maxNumOfCounters";

  public static final String HEALTHCHECK_SETTINGS_PREFIX = "healthcheck";
  public static final String HEALTHCHECK_ENABLED = "enabled";
  public static final String HEALTHCHECK_DELAY = "delayMs";

  public static final String ROUTING_SECONDARY_DATASOURCE = "routing.failedHealthcheck.secondaryDataSource";

  private DataSourceSettings() {
  }
}
