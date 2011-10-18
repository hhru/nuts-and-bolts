package ru.hh.nab.health.limits;

import com.google.common.base.Preconditions;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import ru.hh.nab.health.monitoring.CountingHistogramImpl;

public class HistoLimit implements Limit {
  private final AtomicInteger current;
  private final int max;
  private AtomicLong timestamp;
  private final CountingHistogramImpl<Integer> histo;

  public HistoLimit(int max, CountingHistogramImpl<Integer> histo) {
    this.current = new AtomicInteger(0);
    this.max = max;
    this.histo = histo;
    this.timestamp = new AtomicLong(System.nanoTime());
  }

  @Override
  public boolean acquire() {
    int old = current.get();
    updateHistogram(old);
    for (;;) {
      Preconditions.checkState(old <= max);
      if (old == max)
        return false;
      if (current.weakCompareAndSet(old, old+1))
        return true;
      old = current.get();
    }
  }

  @Override
  public void release() {
    int old = current.get();
    updateHistogram(old);
    for (;;) {
      if (current.weakCompareAndSet(old, old-1))
        break;
      old = current.get();
    }
    Preconditions.checkState(old > 0);
  }

  private void updateHistogram(int old) {
    long now = System.nanoTime();
    long before = timestamp.getAndSet(now);
    histo.put(old, now - before);
  }
}
