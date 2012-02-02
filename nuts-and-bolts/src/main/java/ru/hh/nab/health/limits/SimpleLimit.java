package ru.hh.nab.health.limits;

import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleLimit implements Limit {
  private final int max;
  private final AtomicInteger current = new AtomicInteger(0);
  private final LeakDetector detector;
  private final String name;
  
  private final static Logger LOGGER = LoggerFactory.getLogger(SimpleLimit.class);

  public SimpleLimit(int max, LeakDetector leakDetector, String name) {
    this.max = max;
    this.detector = leakDetector;
    this.name = name;
  }

  @Override
  public LeaseToken acquire() {
    if (current.incrementAndGet() > max) {
      current.decrementAndGet();
      LOGGER.debug("acquired,limit:{},token:-,max,current:{}", name, current);
      return null;
    }

    LeaseToken token = new LeaseToken() {
      @Override
      public void release() {
        detector.released(this);
        current.decrementAndGet();
        LOGGER.debug("released,limit:{},token:{},ok,current:{}", objects(name, hashCode(), current.get()));
      }
    };
    detector.acquired(token);

    LOGGER.debug("acquired,limit:{},token:{},ok,current:{}", objects(name, hashCode(), current.get()));
    return token;
  }

  private static Object[] objects(Object... args) {
    return args;
  }
}
