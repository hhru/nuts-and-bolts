package ru.hh.nab.health.limits;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.Maps;
import com.google.inject.Provider;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.scopes.ScopeClosure;

public class LeakDetector implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(LeakDetector.class);

  static class LeaseContext implements Comparable<LeaseContext> {
    public final long deadline;
    public final ScopeClosure closure;
    public LeaseContext(long deadline, ScopeClosure closure) {
      this.deadline = deadline;
      this.closure = closure;
    }

    @Override
    public final int compareTo(LeaseContext o) {
      if (this == o)
        return 0;
      if (o.deadline == deadline) {
        return this.hashCode() - o.hashCode();
      }
      return deadline > o.deadline ? 1 : -1;
    }
  }

  public static class FirstException extends Exception {
  }

  private final ConcurrentMap<LeaseToken, LeaseContext> leases = Maps.newConcurrentMap();
  private final Cache<LeaseToken, FirstException> stackTraces = CacheBuilder.newBuilder().expireAfterWrite(400, TimeUnit.SECONDS).
          maximumSize(50000).build(new CacheLoader<LeaseToken, FirstException>() {
    @Override
    public FirstException load(LeaseToken key) throws Exception {
      return new FirstException();
    }
  });
  private final NavigableMap<LeaseContext, LeaseToken> byDeadline = new ConcurrentSkipListMap<LeaseContext, LeaseToken>();
  private final Provider<? extends ScopeClosure> requestScopeProvider;
  private final long defaultTtl;
  private final LeakListener leakListener;

  public LeakDetector(long defaultTtl, Provider<? extends ScopeClosure> requestScopeProvider) {
    this(defaultTtl, requestScopeProvider, new DefaultLeakListener());
  }

  public LeakDetector(long defaultTtl, Provider<? extends ScopeClosure> requestScopeProvider, LeakListener leakListener) {
    this.defaultTtl = defaultTtl;
    this.requestScopeProvider = requestScopeProvider;
    this.leakListener = leakListener;
  }

  public void acquired(LeaseToken o) {
    acquired(o, defaultTtl);
  }

  public void acquired(LeaseToken o, long ttl) {
    long now = DateTimeUtils.currentTimeMillis();
    LeaseContext acq = new LeaseContext(now + ttl, requestScopeProvider.get());
    if (leases.putIfAbsent(o, acq) != null)
      LOG.error("Tried to acquire a token twice", new IllegalStateException());
    else
      byDeadline.put(acq, o);
  }

  public void released(LeaseToken o) {
    LeaseContext t = leases.remove(o);
    if (t == null) {
      FirstException firstEx;
      try {
        firstEx = stackTraces.get(o);
      } catch (ExecutionException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
      LOG.error("Tried to release a token twice", new IllegalStateException());
      LOG.error("Tried to release a token twice, first exception was", firstEx);
    } else {
      byDeadline.remove(t);
      try {
        stackTraces.get(o);
      } catch (ExecutionException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }
  }

  public void run() {
    SortedMap<LeaseContext, LeaseToken> expired = byDeadline.headMap(new LeaseContext(DateTimeUtils.currentTimeMillis(), null));
    for (Map.Entry<LeaseContext, LeaseToken> e : expired.entrySet()) {
      LeaseContext ctx = e.getKey();
      ctx.closure.enter();
      try {
        leakListener.leakDetected();
      } finally {
        ctx.closure.leave();
      }
      byDeadline.remove(e.getKey());
    }
  }
}
