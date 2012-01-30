package ru.hh.nab.health.limits;

import java.util.concurrent.atomic.AtomicInteger;

public class SimpleLimit implements Limit {
  private final int max;
  private final AtomicInteger current = new AtomicInteger(0);
  private final LeakDetector detector;

  public SimpleLimit(int max, LeakDetector leakDetector) {
    this.max = max;
    this.detector = leakDetector;
  }

  @Override
  public LeaseToken acquire() {
    if (current.incrementAndGet() > max) {
      current.decrementAndGet();
      return null;
    }

    LeaseToken token = new LeaseToken() {
      @Override
      public void release() {
        detector.released(this);
        current.decrementAndGet();
      }
    };
    detector.acquired(token);
    return token;
  }
}
