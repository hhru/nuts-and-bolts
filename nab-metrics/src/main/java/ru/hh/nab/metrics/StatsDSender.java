package ru.hh.nab.metrics;

import com.timgroup.statsd.StatsDClient;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A glue between aggregators ({@link Counters}, {@link Histogram}, etc.) and StatsDClient.<br/>
 * For each aggregator there is a corresponding method that registers a periodic task.<br/>
 * This task sends snapshot of the aggregator to a monitoring system and resets the aggregator.
 */
public class StatsDSender {
  private static final int DEFAULT_PERIOD_OF_TRANSMISSION_STATS_SECONDS = 60;

  private final StatsDClient statsDClient;
  private final ScheduledExecutorService scheduledExecutorService;
  private final int periodOfTransmissionStatsSeconds;

  public StatsDSender(StatsDClient statsDClient, ScheduledExecutorService scheduledExecutorService) {
    this.statsDClient = statsDClient;
    this.scheduledExecutorService = scheduledExecutorService;
    this.periodOfTransmissionStatsSeconds = DEFAULT_PERIOD_OF_TRANSMISSION_STATS_SECONDS;
  }

  public StatsDSender(StatsDClient statsDClient, ScheduledExecutorService scheduledExecutorService,
                      int periodOfTransmissionStatsSeconds) {
    this.statsDClient = statsDClient;
    this.scheduledExecutorService = scheduledExecutorService;
    this.periodOfTransmissionStatsSeconds = periodOfTransmissionStatsSeconds;
  }

  public void sendPercentilesPeriodically(String metricName, Histogram histogram, int... percentiles) {
    Percentiles percentilesCalculator = new Percentiles(percentiles);
    scheduledExecutorService.scheduleAtFixedRate(
      () -> {
        Map<Integer, Integer> valueToCount = histogram.getValueToCountAndReset();
        computeAndSendPercentiles(metricName, null, valueToCount, percentilesCalculator);
      },
      periodOfTransmissionStatsSeconds,
      periodOfTransmissionStatsSeconds,
      TimeUnit.SECONDS
    );
  }

  public void sendPercentilesPeriodically(String metricName, Histograms histograms, int... percentiles) {
    Percentiles percentilesСalculator = new Percentiles(percentiles);
    scheduledExecutorService.scheduleAtFixedRate(
      () -> {
        Map<Tags, Map<Integer, Integer>> tagsToHistogram = histograms.getTagsToHistogramAndReset();
        for (Map.Entry<Tags, Map<Integer, Integer>> tagsAndHistogram : tagsToHistogram.entrySet()) {
          computeAndSendPercentiles(
            metricName,
            tagsAndHistogram.getKey().getTags(),
            tagsAndHistogram.getValue(),
            percentilesСalculator
          );
        }
      },
      periodOfTransmissionStatsSeconds,
      periodOfTransmissionStatsSeconds,
      TimeUnit.SECONDS
    );
  }

  public void sendTiming(String metricName, long value, Tag... tags) {
    statsDClient.time(getFullMetricName(metricName, tags), value);
  }

  public void sendCounter(String metricName, long delta, Tag... tags) {
    statsDClient.count(getFullMetricName(metricName, tags), delta);
  }

  private void computeAndSendPercentiles(String metricName,
                                         Tag[] tags,
                                         Map<Integer, Integer> valueToCount,
                                         Percentiles percentiles) {
    Map<Integer, Integer> percentileToValue = percentiles.compute(valueToCount);
    for (Map.Entry<Integer, Integer> percentileAndValue : percentileToValue.entrySet()) {
      statsDClient.gauge(
        getFullMetricName(metricName, tags) + ".percentile_is_" + percentileAndValue.getKey(),
        percentileAndValue.getValue()
      );
    }
  }

  public void sendCountersPeriodically(String metricName, Counters counters) {
    scheduledExecutorService.scheduleAtFixedRate(
      () -> sendCountMetric(metricName, counters),
      periodOfTransmissionStatsSeconds,
      periodOfTransmissionStatsSeconds,
      TimeUnit.SECONDS
    );
  }

  public void sendMetricPeriodically(String metricName, Supplier<Long> metric, Tag... tags) {
    scheduledExecutorService.scheduleAtFixedRate(
      () -> sendGaugeMetric(metricName, metric.get(), tags),
      periodOfTransmissionStatsSeconds,
      periodOfTransmissionStatsSeconds,
      TimeUnit.SECONDS
    );
  }

  private void sendCountMetric(String metricName, Counters counters) {
    Map<Tags, Integer> counterAggregatorSnapshot = counters.getSnapshotAndReset();
    counterAggregatorSnapshot.forEach((tags, count) -> statsDClient.count(getFullMetricName(metricName, tags.getTags()), count));
  }

  private void sendGaugeMetric(String metricName, long metric, Tag... tags) {
    statsDClient.gauge(getFullMetricName(metricName, tags), metric);
  }

  public void sendMaxPeriodically(String metricName, Max max, Tag... tags) {
    String fullName = getFullMetricName(metricName, tags);
    scheduledExecutorService.scheduleAtFixedRate(
      () -> statsDClient.gauge(fullName, max.getAndReset()),
      periodOfTransmissionStatsSeconds,
      periodOfTransmissionStatsSeconds,
      TimeUnit.SECONDS
    );
  }

  public void sendMomentsPeriodically(String metricName, Moments moments, Tag... tags) {
    String minMetricName = getFullMetricName(metricName + ".min", tags);
    String maxMetricName = getFullMetricName(metricName + ".max", tags);
    String meanMetricName = getFullMetricName(metricName + ".mean", tags);
    String varianceMetricName = getFullMetricName(metricName + ".variance", tags);

    scheduledExecutorService.scheduleAtFixedRate(
      () -> {
        Moments.MomentsData data = moments.getAndReset();
        setGaugeValue(minMetricName, data.getMin());
        setGaugeValue(maxMetricName, data.getMax());
        setGaugeValue(meanMetricName, data.getMean());
        setGaugeValue(varianceMetricName, data.getVariance());
      },
      periodOfTransmissionStatsSeconds,
      periodOfTransmissionStatsSeconds,
      TimeUnit.SECONDS
    );
  }

  private void setGaugeValue(String name, double value) {
    if (value < 0) {
      statsDClient.gauge(name, 0);
    }
    statsDClient.gauge(name, value);
  }

  static String getFullMetricName(String metricName, Tag[] tags) {
    if (tags == null) {
      return metricName;
    }

    int tagsLength = tags.length;
    if (tagsLength == 0) {
      return metricName;
    }

    StringBuilder stringBuilder = new StringBuilder(metricName + '.');

    for (int i = 0; i < tagsLength; i++) {
      stringBuilder.append(tags[i].name.replace('.', '-'))
        .append("_is_")
        .append(tags[i].value.replace('.', '-'));
      if (i != tagsLength - 1) {
        stringBuilder.append('.');
      }
    }
    return stringBuilder.toString();
  }
}
