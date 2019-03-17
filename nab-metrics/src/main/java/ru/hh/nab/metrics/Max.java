package ru.hh.nab.metrics;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * An aggregator that maintains a maximum value over stream of values.<br/>
 * For example, maximum queue size.
 */
public class Max {

  private static final AtomicIntegerFieldUpdater<Max> maxUpdater = AtomicIntegerFieldUpdater.newUpdater(Max.class, "max");

  private final int defaultValue;
  private volatile int max;

  /**
   * @param defaultValue every value passed to {@link Max#save} must be greater or equal to this default value.
   */
  public Max(int defaultValue) {
    this.defaultValue = defaultValue;
    max = defaultValue;
  }

  public void save(int value) {
    int currentMax = maxUpdater.get(this);
    while (value > currentMax) {
      boolean set = maxUpdater.compareAndSet(this, currentMax, value);
      if (!set) {
        currentMax = maxUpdater.get(this);
      }
    }
  }

  int getAndReset() {
    return maxUpdater.getAndSet(this, defaultValue);
  }

}
