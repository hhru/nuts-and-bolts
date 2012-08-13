package ru.hh.nab.health.limits;

import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class SimpleLimit implements Limit {
  public static final String REQ_H_X_REQUEST_ID = "req.h.x-request-id";
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
    final String requestId = MDC.get(REQ_H_X_REQUEST_ID);
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
        String old = MDC.get(REQ_H_X_REQUEST_ID);
        if (requestId != null)
          MDC.put(REQ_H_X_REQUEST_ID, requestId);
        else if (old != null)
          MDC.remove(REQ_H_X_REQUEST_ID);
        LOGGER.debug("released,limit:{},token:{},ok,current:{}", objects(name, hashCode(), current));
        if (old != null)
          MDC.put(REQ_H_X_REQUEST_ID, old);
        else if (requestId != null)
          MDC.remove(REQ_H_X_REQUEST_ID);
      }
    };
    detector.acquired(token);

    LOGGER.debug("acquired,limit:{},token:{},ok,current:{}", objects(name, token.hashCode(), current));
    return token;
  }

  @Override
  public int getMax() {
    return max;
  }

  @Override
  public String getName() {
    return name;
  }

  private static Object[] objects(Object... args) {
    return args;
  }
}
