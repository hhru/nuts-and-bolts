package ru.hh.nab.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An aggregator that accumulates metrics that can be summed.<br/>
 * For example: number of cache hits, total time of connection usage, etc.<br/>
 */
public class Counters {
  private static final Logger logger = LoggerFactory.getLogger(Counters.class);

  private final Map<Tags, AtomicInteger> tagsToCounter = new ConcurrentHashMap<>();
  private final int maxNumOfCounters;

  /**
   * @param maxNumOfCounters an upper limit on the number of counters.<br/>
   * An instance of Counters maintains a separate counter for each combination of tags.<br/>
   * If there are too many combinations, we can consume too much memory.<br/>
   * To prevent this, when maxNumOfCounters is reached a message will logged to Slf4J and a counter with the least value will be thrown away.
   */
  public Counters(int maxNumOfCounters) {
    this.maxNumOfCounters = maxNumOfCounters;
  }

  public void add(int value, Tag tag) {
    addInner(value, tag);
  }

  public void add(int value, Tag... tagsArr) {
    addInner(value, new MultiTags(tagsArr));
  }

  private void addInner(int value, Tags tags) {
    AtomicInteger counter = tagsToCounter.get(tags);
    if (counter == null) {
      if (tagsToCounter.size() >= maxNumOfCounters) {
        removeSomeCounter();
      }
      counter = tagsToCounter.putIfAbsent(tags, new AtomicInteger(value));
      if (counter == null) {
        return;
      }
    }
    counter.addAndGet(value);
  }

  private void removeSomeCounter() {
    int minValue = Integer.MAX_VALUE;
    Tags tagsToRemove = null;

    for (Map.Entry<Tags, AtomicInteger> entry : tagsToCounter.entrySet()) {
      int curValue = entry.getValue().intValue();
      if (curValue < minValue) {
        tagsToRemove = entry.getKey();
        minValue = curValue;
      }
    }

    tagsToCounter.remove(tagsToRemove);
    logger.error("Max num ({}) of counters reached, removed counter {}", maxNumOfCounters, tagsToRemove);
  }

  Map<Tags, Integer> getSnapshotAndReset() {
    Map<Tags, Integer> tagsToCountSnapshot = new HashMap<>();
    for (Tags tags : tagsToCounter.keySet()) {
      AtomicInteger counter = tagsToCounter.get(tags);
      int count = counter.getAndSet(0);
      if (count == 0) {
        tagsToCounter.remove(tags);
      }
      tagsToCountSnapshot.put(tags, count);
    }
    return tagsToCountSnapshot;
  }
}
