package ru.hh.nab.scopes;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.sun.grizzly.tcp.http11.GrizzlyRequest;
import java.util.Map;
import org.slf4j.MDC;
import ru.hh.health.monitoring.TimingsLogger;

public class RequestScope implements TransferrableScope {
  public static final RequestScope REQUEST_SCOPE = new RequestScope();

  private static final ThreadLocal<RequestScopeClosure> closure = new ThreadLocal<RequestScopeClosure>();

  private static enum NullObject {
    INSTANCE
  }

  public static class RequestContext {
    protected static final String X_REQUEST_ID = "x-request-id";
    protected static final String X_REQUEST_ID_DEFAULT = "NoRequestIdProvided";
    protected static final String X_HHID_PERFORMER = "x-hhid-performer";
    protected static final String X_HHID_PERFORMER_DEFAULT = "NoPerformerTokenProvided";
    protected static final String X_UID = "x-uid";
    protected static final String X_UID_DEFAULT = "NoUidProvided";
    protected static final String REQ_REMOTE_ADDR = "req.remote-addr";
    protected static final String REQ_REMOTE_ADDR_DEFAULT = "NoRemoteAddrProvided";
    private final String requestId;
    private final String performerToken;
    private final String uid;
    private final String remoteAddr;
    private final Object request;
    public RequestContext(String requestId, String performerToken, String uid, String remoteAddr, Object request)  {
      this.requestId = requestId;
      this.performerToken = performerToken;
      this.uid = uid;
      this.remoteAddr = remoteAddr;
      this.request = request;
    }
    RequestContext(GrizzlyRequest request) {
      this(request.getHeader(X_REQUEST_ID),
          request.getHeader(X_HHID_PERFORMER),
          request.getHeader(X_UID),
          request.getRemoteAddr(),
          request
      );
    }

    public void setLoggingContext() {
      storeValue("req.h." + X_REQUEST_ID, requestId, X_REQUEST_ID_DEFAULT);
      storeValue("req.h." + X_HHID_PERFORMER, performerToken, X_HHID_PERFORMER_DEFAULT);
      storeValue("req.h." + X_UID, uid, X_UID_DEFAULT);
      storeValue(REQ_REMOTE_ADDR, remoteAddr, REQ_REMOTE_ADDR_DEFAULT);
    }
    private void storeValue(String name, String value, String defaultValue) {
      MDC.put(name, value != null ? value : defaultValue);
    }
    public void clearLoggingContext() {
      MDC.clear();
    }
    public Object getRequest() {
      return request;
    }
  }

  public static void enter(GrizzlyRequest request, TimingsLogger timingsLogger) {
    enter(new RequestContext(request), timingsLogger);
  }

  public static void enter(RequestContext requestContext, TimingsLogger timingsLogger) {
    new RequestScopeClosure(requestContext, timingsLogger).enter();
  }

  public static void leave() {
    closure.get().leave();
  }

  public static Object currentRequest() {
    RequestScopeClosure cls = closure.get();
    if (cls == null) {
      throw new OutOfScopeException("Out of RequestScope");
    }
    return cls.requestContext.getRequest();
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

    private final RequestContext requestContext;
    private final TimingsLogger timingsLogger;
    private final Map<Key<?>, Object> objects = Maps.newHashMap();

    RequestScopeClosure(RequestContext requestContext, TimingsLogger timingsLogger) {
      this.requestContext = requestContext;
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
      requestContext.setLoggingContext();
      RequestScope.closure.set(this);
      timingsLogger.enterTimedArea();
    }

    @Override
    public void leave() {
      Preconditions.checkState(RequestScope.closure.get() == this);
      timingsLogger.leaveTimedArea();
      requestContext.clearLoggingContext();
      RequestScope.closure.remove();
    }
  }
}
