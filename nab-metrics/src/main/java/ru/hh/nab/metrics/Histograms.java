package ru.hh.nab.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintains a separate {@link Histogram} for each combination of tags.<br/>
 */
public class Histograms {
  private static final Logger LOGGER = LoggerFactory.getLogger(Histograms.class);

  private final Map<Tags, Histogram> tagsToHistogram = new ConcurrentHashMap<>();
  private final int maxNumOfHistograms;
  private final Supplier<Histogram> histogramSupplier;

  /**
   * @param maxHistogramSize   an upper limit on the number of different metric values. See {@link Histogram#Histogram(int)}.
   * @param maxNumOfHistograms an upper limit on the number of histograms.<br/>
   *                           An instance of Histograms maintains a separate {@link Histogram} for each combination of tags.<br/>
   *                           If there are too many combinations we can consume too much memory.<br/>
   *                           To prevent this, when maxNumOfHistograms is reached a message will be logged to Slf4J and a new histogram will be
   *                           thrown away.
   */
  public Histograms(int maxHistogramSize, int maxNumOfHistograms) {
    this(maxHistogramSize, maxNumOfHistograms, HistogramType.SIMPLE);
  }

  /**
   * @param maxHistogramSize   an upper limit on the number of different metric values. See {@link Histogram#Histogram(int)}.
   * @param maxNumOfHistograms an upper limit on the number of histograms.<br/>
   *                           An instance of Histograms maintains a separate {@link Histogram} for each combination of tags.<br/>
   *                           If there are too many combinations we can consume too much memory.<br/>
   *                           To prevent this, when maxNumOfHistograms is reached a message will be logged to Slf4J and a new histogram will be
   *                           thrown away.
   * @param histogramType      type of histogram. Possible values: SIMPLE, RANGE. If you want to use histogram of another type use
   *                           {@link Histograms#Histograms(int, Supplier)} to create Histograms instance
   * @deprecated Use {@link Histograms#Histograms(int, Supplier)}
   */
  @Deprecated(forRemoval = true)
  public Histograms(int maxHistogramSize, int maxNumOfHistograms, HistogramType histogramType) {
    this(
        maxNumOfHistograms,
        histogramType == HistogramType.SIMPLE ? () -> new SimpleHistogram(maxHistogramSize) : () -> new RangedHistogram(maxHistogramSize)
    );
  }

  /**
   * @param maxNumOfHistograms an upper limit on the number of histograms.<br/>
   *                           An instance of Histograms maintains a separate {@link Histogram} for each combination of tags.<br/>
   *                           If there are too many combinations we can consume too much memory.<br/>
   *                           To prevent this, when maxNumOfHistograms is reached a message will be logged to Slf4J and a new histogram will be
   *                           thrown away.
   * @param histogramSupplier  supplier used to create new instance of Histogram for tags combination
   */
  public Histograms(int maxNumOfHistograms, Supplier<Histogram> histogramSupplier) {
    this.maxNumOfHistograms = maxNumOfHistograms;
    this.histogramSupplier = histogramSupplier;
  }

  public void save(int value, Tag tag) {
    saveInner(value, tag);
  }

  public void save(int value, Tag... tags) {
    saveInner(value, new MultiTags(tags));
  }

  private void saveInner(int value, Tags tags) {
    Histogram histogram = tagsToHistogram.get(tags);
    if (histogram == null) {
      if (tagsToHistogram.size() >= getMaxNumOfHistograms()) {
        LOGGER.error("Max number of histograms, dropping observation");
        return;
      }
      histogram = histogramSupplier.get();
      Histogram currentHistogram = tagsToHistogram.putIfAbsent(tags, histogram);
      if (currentHistogram != null) {
        histogram = currentHistogram;
      }
    }
    histogram.save(value);
  }

  Map<Tags, Map<Integer, Integer>> getTagsToHistogramAndReset() {
    Map<Tags, Map<Integer, Integer>> tagsToHistogramSnapshot = new HashMap<>(tagsToHistogram.size());
    for (Map.Entry<Tags, Histogram> entry : tagsToHistogram.entrySet()) {
      Tags tags = entry.getKey();
      Histogram histogram = entry.getValue();
      Map<Integer, Integer> histSnapshot = histogram.getValueToCountAndReset();
      if (!histSnapshot.isEmpty()) {
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

  public enum HistogramType {
    SIMPLE,
    RANGE
  }
}
