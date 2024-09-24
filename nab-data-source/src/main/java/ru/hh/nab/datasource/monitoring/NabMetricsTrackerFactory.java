package ru.hh.nab.datasource.monitoring;

import com.zaxxer.hikari.metrics.IMetricsTracker;
import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import com.zaxxer.hikari.metrics.PoolStats;
import static java.util.Optional.ofNullable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.common.properties.FileSettings;
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
  private final FileSettings dataSourceSettings;

  public NabMetricsTrackerFactory(
      String serviceName,
      StatsDSender statsDSender,
      FileSettings dataSourceSettings
  ) {
    this.serviceName = serviceName;
    this.statsDSender = statsDSender;
    this.dataSourceSettings = dataSourceSettings;
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
      this.samplePoolUsageStats = ofNullable(dataSourceSettings.getBoolean(MONITORING_SAMPLE_POOL_USAGE_STATS)).orElse(Boolean.FALSE);
      this.longConnectionUsageMs = dataSourceSettings.getInteger(MONITORING_LONG_CONNECTION_USAGE_MS);
      this.datasourceTag = new Tag(DATASOURCE_TAG_NAME, poolName);
      this.appTag = new Tag(APP_TAG_NAME, serviceName);
      Tag[] jdbcTags = new Tag[]{datasourceTag, appTag};

      creationHistogram = new CompactHistogram(
          ofNullable(dataSourceSettings.getInteger(MONITORING_CREATION_HISTOGRAM_SIZE)).orElse(2048),
          ofNullable(dataSourceSettings.getInteger(MONITORING_CREATION_HISTOGRAM_COMPACTION_RATIO)).orElse(1)
      );
      acquisitionHistogram = new CompactHistogram(
          ofNullable(dataSourceSettings.getInteger(MONITORING_ACQUISITION_HISTOGRAM_SIZE)).orElse(2048),
          ofNullable(dataSourceSettings.getInteger(MONITORING_ACQUISITION_HISTOGRAM_COMPACTION_RATIO)).orElse(1)
      );
      usageHistogram = new CompactHistogram(
          ofNullable(dataSourceSettings.getInteger(MONITORING_USAGE_HISTOGRAM_SIZE)).orElse(2048),
          ofNullable(dataSourceSettings.getInteger(MONITORING_USAGE_HISTOGRAM_COMPACTION_RATIO)).orElse(1)
      );
      timeoutCounters = new Counters(ofNullable(dataSourceSettings.getInteger(MONITORING_CONNECTION_TIMEOUT_MAX_NUM_OF_COUNTERS)).orElse(500));

      if (samplePoolUsageStats) {
        usageCounters = new Counters(ofNullable(dataSourceSettings.getInteger(MONITORING_SAMPLED_USAGE_MAX_NUM_OF_COUNTERS)).orElse(2000));
      } else {
        usageCounters = new Counters(ofNullable(dataSourceSettings.getInteger(MONITORING_TOTAL_USAGE_MAX_NUM_OF_COUNTERS)).orElse(500));
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
