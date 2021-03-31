package ru.hh.nab.metrics;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * An aggregator that accumulates a stream of values as a histogram to compute percentiles.<br/>
 * For example, response times.
 */
public class UniformHistogram {
  private final AtomicInteger count = new AtomicInteger();
  private final AtomicLongArray values;

  /**
   * @param maxHistogramSize an upper limit on the number of different metric values.<br/>
   * If there are too many different values we can consume too much memory.<br/>
   * To prevent this, when maxHistogramSize is reached, messages will be sampled.
   */
  public UniformHistogram(int maxHistogramSize) {
    this.values = new AtomicLongArray(maxHistogramSize);
  }

  public void save(long value) {
    final long c = count.incrementAndGet();
    if (c <= values.length()) {
      values.set((int) c - 1, value);
    } else {
      final long r = ThreadLocalRandom.current().nextLong(c);
      if (r < values.length()) {
        values.set((int) r, value);
      }
    }
  }

  public long[] getValuesAndReset() {
    int size = values.length();
    long c = count.getAndSet(0);
    if (c < size) {
      size = (int) c;
    }

    long[] data = new long[size];
    for (int i = 0; i < size; i++) {
      data[i] = values.get(i);
    }

    return data;
  }
}

