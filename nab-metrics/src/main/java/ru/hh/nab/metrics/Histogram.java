package ru.hh.nab.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An aggregator that accumulates a stream of values as a histogram to compute percentiles.<br/>
 * For example, response times.
 */
public class Histogram {
  private static final Logger logger = LoggerFactory.getLogger(Histogram.class);

  private final int maxHistogramSize;
  private final Map<Integer, AtomicInteger> valueToCounter;

  /**
   * @param maxHistogramSize an upper limit on the number of different metric values.<br/>
   * An instance of Histogram maintains a separate counter for each metric value.<br/>
   * If there are too many different values we can consume too much memory.<br/>
   * To prevent this, when maxHistogramSize is reached a message will be logged to Slf4J and a new observation will be thrown away.
   */
  public Histogram(int maxHistogramSize) {
    this.maxHistogramSize = maxHistogramSize;
    this.valueToCounter = new ConcurrentHashMap<>(maxHistogramSize);
  }

  public void save(int value) {
    AtomicInteger counter = valueToCounter.get(value);
    if (counter == null) {
      if (valueToCounter.size() >= maxHistogramSize) {
        logger.error("Max number of different values reached, dropping observation");
        return;
      }
      counter = new AtomicInteger(1);
      counter = valueToCounter.putIfAbsent(value, counter);
      if (counter == null) {
        return;
      }
    }
    counter.incrementAndGet();
  }

  public Map<Integer, Integer> getValueToCountAndReset() {
    Map<Integer, Integer> valueToCount = new HashMap<>(valueToCounter.size());
    for (Integer value : valueToCounter.keySet()) {
      AtomicInteger counter = valueToCounter.get(value);
      int count = counter.getAndSet(0);
      if (count > 0) {
        valueToCount.put(value, count);
      } else {
        valueToCounter.remove(value);
      }
    }
    return valueToCount;
  }
}

