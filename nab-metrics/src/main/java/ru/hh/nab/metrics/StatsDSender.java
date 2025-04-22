package ru.hh.nab.metrics;

import com.timgroup.statsd.StatsDClient;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A glue between aggregators ({@link Counters}, {@link Histogram}, etc.) and StatsDClient.<br/>
 * For each aggregator there is a corresponding method that registers a periodic task.<br/>
 * This task sends snapshot of the aggregator to a monitoring system and resets the aggregator.
 */
public class StatsDSender {

  private static final Logger LOGGER = LoggerFactory.getLogger(StatsDSender.class);
  public static final int DEFAULT_SEND_INTERVAL_SECONDS = 60;
  public static final int[] DEFAULT_PERCENTILES = {95, 99, 100};

  private final StatsDClient statsDClient;
  private final ScheduledExecutorService scheduledExecutorService;
  private final int defaultPeriodicSendInterval;

  public StatsDSender(StatsDClient statsDClient, ScheduledExecutorService scheduledExecutorService) {
    this(statsDClient, scheduledExecutorService, DEFAULT_SEND_INTERVAL_SECONDS);
  }

  public StatsDSender(StatsDClient statsDClient, ScheduledExecutorService scheduledExecutorService, int defaultPeriodicSendInterval) {
    this.statsDClient = statsDClient;
    this.scheduledExecutorService = scheduledExecutorService;
    this.defaultPeriodicSendInterval = defaultPeriodicSendInterval > 0 ? defaultPeriodicSendInterval : DEFAULT_SEND_INTERVAL_SECONDS;
  }

  public void sendTime(String metricName, long value, Tag... tags) {
    statsDClient.time(getFullMetricName(metricName, tags), value);
  }

  public void sendCount(String metricName, long delta, Tag... tags) {
    statsDClient.count(getFullMetricName(metricName, tags), delta);
  }

  public void sendCounters(String metricName, Counters counters, Tag... additionalTags) {
    Map<Tags, Integer> counterAggregatorSnapshot = counters.getSnapshotAndReset();
    counterAggregatorSnapshot.forEach(
        (counterTags, count) -> statsDClient.count(getFullMetricName(metricName, counterTags.getTags(), additionalTags), count)
    );
  }

  public void sendLongCounters(String metricName, LongCounters counters) {
    Map<Tags, Long> counterAggregatorSnapshot = counters.getSnapshotAndReset();
    counterAggregatorSnapshot.forEach((tags, count) -> statsDClient.count(getFullMetricName(metricName, tags.getTags()), count));
  }

  public void sendGauge(String metricName, long metric, Tag... tags) {
    statsDClient.gauge(getFullMetricName(metricName, tags), metric);
  }

  public void sendGauge(String metricName, double metric, Tag... tags) {
    statsDClient.gauge(getFullMetricName(metricName, tags), metric);
  }

  public void sendSetValue(String metricName, String metric, Tag... tags) {
    statsDClient.recordSetValue(getFullMetricName(metricName, tags), metric);
  }

  public void sendMax(String metricName, Max max, Tag... tags) {
    statsDClient.gauge(getFullMetricName(metricName, tags), max.getAndReset());
  }

  public void sendHistogram(String metricName, Histogram histogram, int... percentiles) {
    computeAndSendPercentiles(metricName, null, histogram.getValueToCountAndReset(), percentiles);
  }

  public void sendHistogram(String metricName, Tag[] tags, Histogram histogram, int... percentiles) {
    computeAndSendPercentiles(metricName, tags, histogram.getValueToCountAndReset(), percentiles);
  }

  public void sendHistograms(String metricName, Histograms histograms, int... percentiles) {
    Map<Tags, Map<Integer, Integer>> tagsToHistogram = histograms.getTagsToHistogramAndReset();
    for (Map.Entry<Tags, Map<Integer, Integer>> tagsAndHistogram : tagsToHistogram.entrySet()) {
      computeAndSendPercentiles(
          metricName,
          tagsAndHistogram.getKey().getTags(),
          tagsAndHistogram.getValue(),
          percentiles
      );
    }
  }

  public void sendUniformHistogram(String metricName, UniformHistogram histogram, int... percentiles) {
    computeAndSendPercentiles(metricName, null, histogram.getValuesAndReset(), percentiles);
  }

  public void sendUniformHistogram(String metricName, Tag[] tags, UniformHistogram histogram, int... percentiles) {
    computeAndSendPercentiles(metricName, tags, histogram.getValuesAndReset(), percentiles);
  }

  public void sendUniformHistograms(String metricName, UniformHistograms histograms, int... percentiles) {
    Map<Tags, long[]> tagsToHistogram = histograms.getTagsToHistogramAndReset();
    for (Map.Entry<Tags, long[]> tagsAndHistogram : tagsToHistogram.entrySet()) {
      computeAndSendPercentiles(
          metricName,
          tagsAndHistogram.getKey().getTags(),
          tagsAndHistogram.getValue(),
          percentiles
      );
    }
  }

  private void computeAndSendPercentiles(String metricName, Tag[] tags, Map<Integer, Integer> valueToCount, int... percentiles) {
    Map<Integer, Integer> percentileToValue = Percentiles.computePercentiles(valueToCount, percentiles);
    for (Map.Entry<Integer, Integer> percentileAndValue : percentileToValue.entrySet()) {
      statsDClient.gauge(
          getFullMetricName(metricName, tags) + ".percentile_is_" + percentileAndValue.getKey(),
          percentileAndValue.getValue()
      );
    }
  }

  private void computeAndSendPercentiles(String metricName, Tag[] tags, long[] values, int... percentiles) {
    Map<Integer, Long> percentileToValue = Percentiles.computePercentiles(values, percentiles);
    for (Map.Entry<Integer, Long> percentileAndValue : percentileToValue.entrySet()) {
      statsDClient.gauge(
          getFullMetricName(metricName, tags) + ".percentile_is_" + percentileAndValue.getKey(),
          percentileAndValue.getValue()
      );
    }
  }

  public void sendMoments(String metricName, Moments moments, Tag... tags) {
    Moments.MomentsData data = moments.getAndReset();
    setGaugeValue(getFullMetricName(metricName + ".min", tags), data.getMin());
    setGaugeValue(getFullMetricName(metricName + ".max", tags), data.getMax());
    setGaugeValue(getFullMetricName(metricName + ".mean", tags), data.getMean());
    setGaugeValue(getFullMetricName(metricName + ".variance", tags), data.getVariance());
  }

  private void setGaugeValue(String name, double value) {
    if (value < 0) {
      statsDClient.gauge(name, 0);
    }
    statsDClient.gauge(name, value);
  }

  public void sendPeriodically(Runnable command) {
    sendPeriodically(command, defaultPeriodicSendInterval);
  }

  public void sendPeriodically(Runnable command, int sendIntervalSeconds) {
    scheduledExecutorService.scheduleAtFixedRate(command, sendIntervalSeconds, sendIntervalSeconds, TimeUnit.SECONDS);
  }

  static String getFullMetricName(String metricName, Tag[] tags) {
    return getFullMetricName(metricName, tags, null);
  }

  static String getFullMetricName(String metricName, Tag[] tags, Tag[] additionalTags) {
    if (isEmpty(tags) && isEmpty(additionalTags)) {
      return metricName;
    }

    var stringBuilder = new StringBuilder(metricName);
    appendTags(metricName, stringBuilder, tags);
    appendTags(metricName, stringBuilder, additionalTags);
    return stringBuilder.toString();
  }

  private static void appendTags(String metricName, StringBuilder stringBuilder, Tag[] tags) {
    if (isEmpty(tags)) {
      return;
    }

    for (var tag : tags) {
      if (tag.name == null) {
        LOGGER.warn("Null tag name for metric: {}", metricName);
        continue;
      }
      if (tag.value == null) {
        LOGGER.warn("Null tag value for tag name: {}, for metric: {}", tag.name, metricName);
      }

      stringBuilder
          .append('.')
          .append(tag.name.replace('.', '-'))
          .append("_is_")
          .append(Optional.ofNullable(tag.value).map(value -> value.replace('.', '-')).orElse("null"));
    }
  }

  private static <T> boolean isEmpty(T[] array) {
    return array == null || array.length == 0;
  }
}
