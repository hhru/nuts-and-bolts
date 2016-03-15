package ru.hh.nab.scopes;

import com.google.common.base.Preconditions;
import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.hh.health.monitoring.TimingsLogger;
import ru.hh.health.monitoring.TimingsLoggerFactory;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RequestScope implements TransferrableScope {
  public static final RequestScope REQUEST_SCOPE = new RequestScope();

  @Inject
  private static TimingsLoggerFactory timingsLoggerFactory;

  private static final ThreadLocal<RequestScopeClosure> closure = new ThreadLocal<>();

  private static final Logger logger = LoggerFactory.getLogger(RequestScope.class);

  private enum NullObject {
    INSTANCE
  }

  public static class RequestContext {
    public static final String X_REQUEST_ID = "x-request-id";
    protected static final String X_REQUEST_ID_DEFAULT = "NoRequestIdProvided";
    protected static final String X_HHID_PERFORMER = "x-hhid-performer";
    protected static final String X_HHID_PERFORMER_DEFAULT = "NoPerformerTokenProvided";
    protected static final String X_UID = "x-uid";
    protected static final String X_UID_DEFAULT = "NoUidProvided";
    protected static final String REQ_REMOTE_ADDR = "req.remote-addr";
    protected static final String REQ_REMOTE_ADDR_DEFAULT = "NoRemoteAddrProvided";

    private static final String NO_REQUEST_ID = "NoRequestId";

    private final String requestId;
    private final String performerToken;
    private final String uid;
    private final String remoteAddr;
    private final Object request;
    private final Object response;

    public RequestContext(String requestId, String performerToken, String uid, String remoteAddr, Object request, Object response)  {
      this.requestId = requestId;
      this.performerToken = performerToken;
      this.uid = uid;
      this.remoteAddr = remoteAddr;
      this.request = request;
      this.response = response;
    }

    RequestContext(HttpServletRequest request, HttpServletResponse response) {
      this(request.getHeader(X_REQUEST_ID) == null ? NO_REQUEST_ID : request.getHeader(X_REQUEST_ID),
        request.getHeader(X_HHID_PERFORMER),
        request.getHeader(X_UID),
        request.getRemoteAddr(),
        request,
        response
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
    public Object getResponse() {
      return response;
    }
  }

  public static void enter(HttpServletRequest request, HttpServletResponse response) {
    String loggerContext = String.format("%s %s", request.getMethod(), request.getRequestURI());
    enter(new RequestContext(request, response), loggerContext);
  }

  public static void enter(RequestContext requestContext, String context) {
    final TimingsLogger timingsLogger = timingsLoggerFactory.getLogger(context, requestContext.requestId);
    timingsLogger.enterTimedArea();
    new RequestScopeClosure(requestContext, timingsLogger).enter();
    RequestScope.addAfterServiceTask(() -> {
        timingsLogger.leaveTimedArea();
        return null;
      }
    );
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

  public static Object currentResponse() {
    RequestScopeClosure cls = closure.get();
    if (cls == null) {
      throw new OutOfScopeException("Out of RequestScope");
    }
    return cls.requestContext.getResponse();
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

  public static void incrementAfterServiceLatchCounter() {
    RequestScopeClosure cls = closure.get();
    if (cls == null) {
      throw new OutOfScopeException("Out of RequestScope");
    }
    cls.incrementAfterServiceTasksLatchCounter();
  }

  public static void decrementAfterServiceLatchCounter() {
    RequestScopeClosure cls = closure.get();
    if (cls == null) {
      throw new OutOfScopeException("Out of RequestScope");
    }
    cls.decrementAfterServiceTasksLatchCounter();
  }

  public static void addAfterServiceTask(Callable<Void> task) {
    RequestScopeClosure cls = closure.get();
    if (cls == null) {
      throw new OutOfScopeException("Out of RequestScope");
    }
    cls.afterServiceTasks.add(task);
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
    private final Map<Key<?>, Object> objects = new HashMap<>();

    // The latch counter is increased when there is a reason to hold off
    // running afterServiceTasks, i.e. for example web server response is suspended but we
    // are already in scope and can not enter it the second time, and there is a continuation
    // task waiting to be run in another thread pool, so there may be a little period where
    // request is not exactly complete but no threads are in the 'entered' into the scope state.
    private int afterServiceTasksLatch = 0;
    private final List<Callable<Void>> afterServiceTasks = new ArrayList<Callable<Void>>();

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
    public synchronized void enter() {
      prepareDelayedEnter();
      executeDelayedEnter();
    }

    // call before submitting task to be executed in another thread
    public synchronized void prepareDelayedEnter() {
      timingsLogger.enterTimedArea();
      incrementAfterServiceTasksLatchCounter();
    }

    // call when starting task submitted when calling prapareDelayedEnter()
    public synchronized void executeDelayedEnter() {
      Preconditions.checkState(RequestScope.closure.get() == null);
      requestContext.setLoggingContext();
      RequestScope.closure.set(this);
    }

    // call after prepareDelayedEnter() if was unable to submit task
    public synchronized void cancelDelayedEnter() {
      decrementAfterServiceTasksLatchCounter();
      timingsLogger.leaveTimedArea();
    }

    @Override
    public synchronized void leave() {
      Preconditions.checkState(RequestScope.closure.get() == this);
      decrementAfterServiceTasksLatchCounter();
      timingsLogger.leaveTimedArea();
      requestContext.clearLoggingContext();
      RequestScope.closure.remove();
    }

    private void incrementAfterServiceTasksLatchCounter() {
      afterServiceTasksLatch++;
    }

    private void decrementAfterServiceTasksLatchCounter() {
      afterServiceTasksLatch--;
      if (afterServiceTasksLatch == 0) {
        Collections.reverse(afterServiceTasks);
        for (Callable<Void> task : afterServiceTasks) {
          try {
            task.call();
          } catch (Exception e) {
            logger.error("Error during after service task", e);
          }
        }
      }
    }
  }
}
