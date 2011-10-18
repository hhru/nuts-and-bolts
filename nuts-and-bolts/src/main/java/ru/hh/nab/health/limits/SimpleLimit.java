package ru.hh.nab.health.limits;

import com.google.common.base.Preconditions;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleLimit implements Limit {
  private final int max;
  private final AtomicInteger current = new AtomicInteger(0);

  public SimpleLimit(int max) {
    this.max = max;
  }

  @Override
  public boolean acquire() {
    if (current.incrementAndGet() > max) {
      current.decrementAndGet();
      return false;
    }
    return true;
  }

  @Override
  public void release() {
    current.decrementAndGet();
    Preconditions.checkState(current.get() >= 0);
  }
}
