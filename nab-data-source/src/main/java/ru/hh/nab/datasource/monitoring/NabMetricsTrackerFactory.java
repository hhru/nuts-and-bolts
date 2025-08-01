package ru.hh.nab.datasource.monitoring;

import com.zaxxer.hikari.metrics.IMetricsTracker;
import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import com.zaxxer.hikari.metrics.PoolStats;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.common.properties.PropertiesUtils;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_ACQUISITION_HISTOGRAM_COMPACTION_RATIO;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_ACQUISITION_HISTOGRAM_SIZE;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_CONNECTION_TIMEOUT_MAX_NUM_OF_COUNTERS;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_CREATION_HISTOGRAM_COMPACTION_RATIO;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_CREATION_HISTOGRAM_SIZE;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_LONG_CONNECTION_USAGE_MS;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_SAMPLED_USAGE_MAX_NUM_OF_COUNTERS;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_SAMPLE_POOL_USAGE_STATS;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_TOTAL_USAGE_MAX_NUM_OF_COUNTERS;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_USAGE_HISTOGRAM_COMPACTION_RATIO;
import static ru.hh.nab.datasource.DataSourceSettings.MONITORING_USAGE_HISTOGRAM_SIZE;
import static ru.hh.nab.datasource.monitoring.ConnectionPoolMetrics.ACQUISITION_MS;
import static ru.hh.nab.datasource.monitoring.ConnectionPoolMetrics.ACTIVE_CONNECTIONS;
import static ru.hh.nab.datasource.monitoring.ConnectionPoolMetrics.CONNECTION_TIMEOUTS;
import static ru.hh.nab.datasource.monitoring.ConnectionPoolMetrics.CREATION_MS;
import static ru.hh.nab.datasource.monitoring.ConnectionPoolMetrics.IDLE_CONNECTIONS;
import static ru.hh.nab.datasource.monitoring.ConnectionPoolMetrics.MAX_CONNECTIONS;
import static ru.hh.nab.datasource.monitoring.ConnectionPoolMetrics.MIN_CONNECTIONS;
import static ru.hh.nab.datasource.monitoring.ConnectionPoolMetrics.PENDING_THREADS;
import static ru.hh.nab.datasource.monitoring.ConnectionPoolMetrics.SAMPLED_USAGE_MS;
import static ru.hh.nab.datasource.monitoring.ConnectionPoolMetrics.TOTAL_CONNECTIONS;
import static ru.hh.nab.datasource.monitoring.ConnectionPoolMetrics.TOTAL_USAGE_MS;
import static ru.hh.nab.datasource.monitoring.ConnectionPoolMetrics.USAGE_MS;
import ru.hh.nab.metrics.CompactHistogram;
import ru.hh.nab.metrics.Counters;
import ru.hh.nab.metrics.Histogram;
import ru.hh.nab.metrics.StatsDSender;
import static ru.hh.nab.metrics.StatsDSender.DEFAULT_PERCENTILES;
import ru.hh.nab.metrics.Tag;
import static ru.hh.nab.metrics.Tag.APP_TAG_NAME;
import static ru.hh.nab.metrics.Tag.DATASOURCE_TAG_NAME;

public class NabMetricsTrackerFactory implements MetricsTrackerFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(NabMetricsTrackerFactory.class);

  private final String serviceName;
  private final StatsDSender statsDSender;
  private final Properties dataSourceProperties;

  public NabMetricsTrackerFactory(
      String serviceName,
      StatsDSender statsDSender,
      Properties dataSourceProperties
  ) {
    this.serviceName = serviceName;
    this.statsDSender = statsDSender;
    this.dataSourceProperties = dataSourceProperties;
  }

  @Override
  public IMetricsTracker create(String poolName, PoolStats poolStats) {
    return new MonitoringMetricsTracker(poolName, poolStats);
  }

  class MonitoringMetricsTracker implements IMetricsTracker {
    private final String poolName;
    private final boolean samplePoolUsageStats;
    private final Integer longConnectionUsageMs;
    private final Counters usageCounters;
    private final Counters timeoutCounters;
    private final Histogram creationHistogram;
    private final Histogram acquisitionHistogram;
    private final Histogram usageHistogram;
    private final Tag datasourceTag;
    private final Tag appTag;

    MonitoringMetricsTracker(String poolName, PoolStats poolStats) {
      this.poolName = poolName;
      this.samplePoolUsageStats = PropertiesUtils.getBoolean(dataSourceProperties, MONITORING_SAMPLE_POOL_USAGE_STATS, Boolean.FALSE);
      this.longConnectionUsageMs = PropertiesUtils.getInteger(dataSourceProperties, MONITORING_LONG_CONNECTION_USAGE_MS);
      this.datasourceTag = new Tag(DATASOURCE_TAG_NAME, poolName);
      this.appTag = new Tag(APP_TAG_NAME, serviceName);
      Tag[] jdbcTags = new Tag[]{datasourceTag, appTag};

      creationHistogram = new CompactHistogram(
          PropertiesUtils.getInteger(dataSourceProperties, MONITORING_CREATION_HISTOGRAM_SIZE, 2048),
          PropertiesUtils.getInteger(dataSourceProperties, MONITORING_CREATION_HISTOGRAM_COMPACTION_RATIO, 1)
      );
      acquisitionHistogram = new CompactHistogram(
          PropertiesUtils.getInteger(dataSourceProperties, MONITORING_ACQUISITION_HISTOGRAM_SIZE, 2048),
          PropertiesUtils.getInteger(dataSourceProperties, MONITORING_ACQUISITION_HISTOGRAM_COMPACTION_RATIO, 1)
      );
      usageHistogram = new CompactHistogram(
          PropertiesUtils.getInteger(dataSourceProperties, MONITORING_USAGE_HISTOGRAM_SIZE, 2048),
          PropertiesUtils.getInteger(dataSourceProperties, MONITORING_USAGE_HISTOGRAM_COMPACTION_RATIO, 1)
      );
      timeoutCounters = new Counters(
          PropertiesUtils.getInteger(dataSourceProperties, MONITORING_CONNECTION_TIMEOUT_MAX_NUM_OF_COUNTERS, 500)
      );

      if (samplePoolUsageStats) {
        usageCounters = new Counters(
            PropertiesUtils.getInteger(dataSourceProperties, MONITORING_SAMPLED_USAGE_MAX_NUM_OF_COUNTERS, 2000)
        );
      } else {
        usageCounters = new Counters(
            PropertiesUtils.getInteger(dataSourceProperties, MONITORING_TOTAL_USAGE_MAX_NUM_OF_COUNTERS, 500)
        );
      }

      statsDSender.sendPeriodically(() -> {
        statsDSender.sendHistogram(CREATION_MS, jdbcTags, creationHistogram, DEFAULT_PERCENTILES);
        statsDSender.sendHistogram(ACQUISITION_MS, jdbcTags, acquisitionHistogram, DEFAULT_PERCENTILES);
        statsDSender.sendHistogram(USAGE_MS, jdbcTags, usageHistogram, DEFAULT_PERCENTILES);
        statsDSender.sendCounters(CONNECTION_TIMEOUTS, timeoutCounters);

        statsDSender.sendGauge(ACTIVE_CONNECTIONS, poolStats.getActiveConnections(), jdbcTags);
        statsDSender.sendGauge(TOTAL_CONNECTIONS, poolStats.getTotalConnections(), jdbcTags);
        statsDSender.sendGauge(IDLE_CONNECTIONS, poolStats.getIdleConnections(), jdbcTags);
        statsDSender.sendGauge(MAX_CONNECTIONS, poolStats.getMaxConnections(), jdbcTags);
        statsDSender.sendGauge(MIN_CONNECTIONS, poolStats.getMinConnections(), jdbcTags);
        statsDSender.sendGauge(PENDING_THREADS, poolStats.getPendingThreads(), jdbcTags);

        if (samplePoolUsageStats) {
          statsDSender.sendCounters(SAMPLED_USAGE_MS, usageCounters);
        } else {
          statsDSender.sendCounters(TOTAL_USAGE_MS, usageCounters);
        }
      });
    }

    @Override
    public void recordConnectionCreatedMillis(long connectionCreatedMillis) {
      creationHistogram.save((int) connectionCreatedMillis);
    }

    @Override
    public void recordConnectionAcquiredNanos(final long elapsedAcquiredNanos) {
      acquisitionHistogram.save((int) TimeUnit.NANOSECONDS.toMillis(elapsedAcquiredNanos));
    }

    @Override
    public void recordConnectionUsageMillis(final long elapsedBorrowedMillis) {
      int connectionUsageMs = (int) elapsedBorrowedMillis;

      if (longConnectionUsageMs != null && connectionUsageMs >= longConnectionUsageMs) {
        String message = String.format(
            "%s connection was used for more than %d ms (%d ms), not fatal, but should be fixed",
            poolName,
            longConnectionUsageMs,
            connectionUsageMs
        );
        LOGGER.error(message, new RuntimeException(poolName + " connection usage duration exceeded"));
      }
      Tag[] tags = new Tag[]{datasourceTag, appTag};
      usageHistogram.save(connectionUsageMs);

      if (!samplePoolUsageStats || ThreadLocalRandom.current().nextInt(100) == 0) {
        usageCounters.add(connectionUsageMs, tags);
      }
    }

    @Override
    public void recordConnectionTimeout() {
      timeoutCounters.add(1, datasourceTag, appTag);
    }
  }
}
