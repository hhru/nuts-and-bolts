package ru.hh.nab.health.monitoring;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLongArray;

public final class CountingHistogramImpl<T> implements Dumpable, CountingHistogram<T> {
  private final AtomicLongArray histo;
  private final BucketMapper<T> mapper;

  public CountingHistogramImpl(BucketMapper<T> mapper) {
    this.mapper = mapper;
    histo = new AtomicLongArray(mapper.max());
  }

  public void put(T value, long amount) {
    histo.addAndGet(mapper.bucket(value), amount);
  }

  public long[] getAndReset() {
    int max = mapper.max();
    long[] ret = new long[max];
    for (int i = 0; i < max; i++) {
      ret[i] = histo.getAndSet(i, 0);
    }
    return ret;
  }

  @Override
  public void dumpAndReset(Appendable out) throws IOException {
    long[] dump = getAndReset();
    for (int i = 0; i < dump.length; i++) {
      out.append(String.format("%d: %d\n", i, dump[i]));
    }
  }
}
