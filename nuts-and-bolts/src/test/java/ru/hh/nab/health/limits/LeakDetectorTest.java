package ru.hh.nab.health.limits;

import com.google.inject.Provider;
import org.joda.time.DateTimeUtils;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import ru.hh.nab.scopes.ScopeClosure;

public class LeakDetectorTest {

  Provider<ScopeClosure> requestScopeProvider = new Provider<ScopeClosure>() {
    @Override
    public ScopeClosure get() {
      return new ScopeClosure() {

        @Override
        public void enter() {
        }

        @Override
        public void leave() {
        }
      };
    }
  };

  private static class TestLeakListener implements LeakListener {
    public boolean leakDetected;

    @Override
    public void leakDetected() {
      leakDetected = true;
    }
  }

  @Test
  public void leakDetected() {
    TestLeakListener leakListener = new TestLeakListener();    
    LeakDetector leakDetector = new LeakDetector(10, requestScopeProvider, leakListener);
    Limit limit = new SimpleLimit(2, leakDetector);
    DateTimeUtils.setCurrentMillisFixed(1);
    LeaseToken token = limit.acquire();
    leakDetector.run();
    assertEquals(false, leakListener.leakDetected);
    DateTimeUtils.setCurrentMillisFixed(12);
    leakDetector.run();
    assertEquals(true, leakListener.leakDetected);
  }

  @Test
  public void release() {
    TestLeakListener leakListener = new TestLeakListener();
    LeakDetector leakDetector = new LeakDetector(10, requestScopeProvider, leakListener);
    Limit limit = new SimpleLimit(2, leakDetector);
    DateTimeUtils.setCurrentMillisFixed(1);
    LeaseToken token = limit.acquire();
    token.release();
    DateTimeUtils.setCurrentMillisFixed(12);
    leakDetector.run();
    assertEquals(false, leakListener.leakDetected);
  }
}
