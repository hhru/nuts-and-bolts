package ru.hh.nab.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains a separate {@link Histogram} for each combination of tags.<br/>
 */
public class UniformHistograms {
  private static final Logger LOGGER = LoggerFactory.getLogger(UniformHistograms.class);

  private final int maxHistogramSize;
  private final Map<Tags, UniformHistogram> tagsToHistogram = new ConcurrentHashMap<>();
  private final int maxNumOfHistograms;

  /**
   * @param maxHistogramSize an upper limit on the number of different metric values. See {@link Histogram#Histogram(int)}.
   * @param maxNumOfHistograms an upper limit on the number of histograms.<br/>
   * An instance of Histograms maintains a separate {@link Histogram} for each combination of tags.<br/>
   * If there are too many combinations we can consume too much memory.<br/>
   * To prevent this, when maxNumOfHistograms is reached, messages will be samples.
   */
  public UniformHistograms(int maxHistogramSize, int maxNumOfHistograms) {
    this.maxHistogramSize = maxHistogramSize;
    this.maxNumOfHistograms = maxNumOfHistograms;
  }

  public void save(long value, Tag tag) {
    saveInner(value, tag);
  }

  public void save(long value, Tag... tags) {
    saveInner(value, new MultiTags(tags));
  }

  private void saveInner(long value, Tags tags) {
    UniformHistogram histogram = tagsToHistogram.get(tags);
    if (histogram == null) {
      if (tagsToHistogram.size() >= getMaxNumOfHistograms()) {
        LOGGER.error("Max number of histograms, dropping observation");
        return;
      }
      histogram = new UniformHistogram(maxHistogramSize);
      UniformHistogram currentHistogram = tagsToHistogram.putIfAbsent(tags, histogram);
      if (currentHistogram != null) {
        histogram = currentHistogram;
      }
    }
    histogram.save(value);
  }

  Map<Tags, long[]> getTagsToHistogramAndReset() {
    Map<Tags, long[]> tagsToHistogramSnapshot = new HashMap<>(tagsToHistogram.size());
    for (Map.Entry<Tags, UniformHistogram> entry : tagsToHistogram.entrySet()) {
      Tags tags = entry.getKey();
      UniformHistogram histogram = entry.getValue();
      long[] histSnapshot = histogram.getValuesAndReset();
      if (histSnapshot.length != 0) {
        tagsToHistogramSnapshot.put(tags, histSnapshot);
      } else {
        tagsToHistogram.remove(tags);
      }
    }
    return tagsToHistogramSnapshot;
  }

  protected int getMaxNumOfHistograms() {
    return maxNumOfHistograms;
  }
}
