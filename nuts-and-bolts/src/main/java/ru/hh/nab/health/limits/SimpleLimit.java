package ru.hh.nab.health.limits;

import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleLimit implements Limit {
  private final int max;
  private final AtomicInteger current = new AtomicInteger(0);
  private final LeakDetector detector;

  private final static Logger LOGGER = LoggerFactory.getLogger(SimpleLimit.class);

  public SimpleLimit(int max, LeakDetector leakDetector) {
    this.max = max;
    this.detector = leakDetector;
  }

  @Override
  public LeaseToken acquire() {
    if (current.incrementAndGet() > max) {
      current.decrementAndGet();
      LOGGER.debug("[{}] max reached: {}", hashCode(), current);
      return null;
    }

    LeaseToken token = new LeaseToken() {
      @Override
      public void release() {
        detector.released(this);
        current.decrementAndGet();
        LOGGER.debug("[{}] released, current: {}", SimpleLimit.this.hashCode(), current.get());
      }
    };
    detector.acquired(token);

    LOGGER.debug("[{}] current: {}", hashCode(), current.get());
    return token;
  }
}
