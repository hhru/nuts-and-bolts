package ru.hh.nab.scopes;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.sun.grizzly.tcp.http11.GrizzlyRequest;
import java.util.Map;
import org.slf4j.MDC;
import ru.hh.nab.health.monitoring.TimingsLogger;

public class RequestScope implements TransferrableScope {
  public static final RequestScope REQUEST_SCOPE = new RequestScope();

  private static final ThreadLocal<RequestScopeClosure> closure = new ThreadLocal<RequestScopeClosure>();

  private static enum NullObject {
    INSTANCE
  }

  public static void enter(GrizzlyRequest req, TimingsLogger timingsLogger) {
    new RequestScopeClosure(req, timingsLogger).enter();
  }

  public static void leave() {
    closure.get().leave();
  }

  public static GrizzlyRequest currentRequest() {
    RequestScopeClosure cls = closure.get();
    if (cls == null) {
      throw new OutOfScopeException("Out of RequestScope");
    }
    return cls.request;
  }

  public static RequestScopeClosure currentClosure() {
    RequestScopeClosure cls = closure.get();
    if (cls == null) {
      throw new OutOfScopeException("Out of RequestScope");
    }
    return cls;
  }

  public static TimingsLogger currentTimingsLogger() {
    RequestScopeClosure cls = closure.get();
    if (cls == null) {
      throw new OutOfScopeException("Out of RequestScope");
    }
    return cls.timingsLogger;
  }

  @Override
  public <T> Provider<T> scope(final Key<T> key, final Provider<T> creator) {
    return new Provider<T>() {
      @Override
      public T get() {
        RequestScopeClosure cls = closure.get();
        synchronized (cls) {
          T t = cls.get(key);
          if (t == null) {
            t = creator.get();
            cls.put(key, t);
          }
          return t;
        }
      }

      @Override
      public String toString() {
        return String.format("%s[%s]", creator, REQUEST_SCOPE);
      }
    };
  }

  @Override
  public ScopeClosure capture() {
    ScopeClosure ret = closure.get();
    Preconditions.checkState(ret != null);
    return ret;
  }

  public static class RequestScopeClosure implements ScopeClosure {
    private static final String X_REQUEST_ID = "x-request-id";
    private static final String X_HHID_PERFORMER = "x-hhid-performer";
    private static final String X_UID = "x-uid";
    private static final String REQ_REMOTE_ADDR = "req.remote-addr";

    private final GrizzlyRequest request;
    private final TimingsLogger timingsLogger;
    private final Map<Key<?>, Object> objects = Maps.newHashMap();

    RequestScopeClosure(GrizzlyRequest request, TimingsLogger timingsLogger) {
      this.request = request;
      this.timingsLogger = timingsLogger;
    }

    @SuppressWarnings({ "unchecked" })
    private <T> T get(Key<T> key) {
      Object o = objects.get(key);
      if (o == NullObject.INSTANCE) {
        return null;
      }
      return (T) o;
    }

    private <T> void put(Key<T> key, T o) {
      objects.put(key, (o != null) ? o : NullObject.INSTANCE);
    }

    @Override
    public void enter() {
      Preconditions.checkState(RequestScope.closure.get() == null);
      storeHeaderValue(request, X_REQUEST_ID);
      storeHeaderValue(request, X_HHID_PERFORMER);
      storeHeaderValue(request, X_UID);
      MDC.put(REQ_REMOTE_ADDR, request.getRemoteAddr());
      RequestScope.closure.set(this);
      timingsLogger.enterTimedArea();
    }

    private void storeHeaderValue(GrizzlyRequest req, String header) {
      MDC.put("req.h." + header, req.getHeader(header));
    }

    @Override
    public void leave() {
      Preconditions.checkState(RequestScope.closure.get() == this);
      timingsLogger.leaveTimedArea();
      MDC.clear();
      RequestScope.closure.remove();
    }
  }
}
