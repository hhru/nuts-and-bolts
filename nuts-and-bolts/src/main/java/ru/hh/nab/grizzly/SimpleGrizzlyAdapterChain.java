package ru.hh.nab.grizzly;

import com.google.common.collect.Lists;
import com.sun.grizzly.tcp.Request;
import com.sun.grizzly.tcp.http11.GrizzlyAdapter;
import com.sun.grizzly.tcp.http11.GrizzlyRequest;
import com.sun.grizzly.tcp.http11.GrizzlyResponse;
import java.util.List;
import ru.hh.nab.health.monitoring.TimingsLogger;
import ru.hh.nab.health.monitoring.TimingsLoggerFactory;
import ru.hh.nab.scopes.RequestScope;

public class SimpleGrizzlyAdapterChain extends GrizzlyAdapter {
  private final static String INVOKED_ADAPTER_FLAG = SimpleGrizzlyAdapterChain.class.getName() + ".invokedAdapter";
  // XXX: Achtung!
  private final static int ADAPTER_ABSTAINED_NOTE = 29;

  private final TimingsLoggerFactory timingsLoggerFactory;

  private final static String X_REQUEST_ID = "x-request-id";

  public SimpleGrizzlyAdapterChain(TimingsLoggerFactory timingsLoggerFactory) {
    this.timingsLoggerFactory = timingsLoggerFactory;
  }

  public static void abstain(Request r) {
    r.setNote(ADAPTER_ABSTAINED_NOTE, true);
  }

  public static void abstain(GrizzlyRequest r) {
    abstain(r.getRequest());
  }

  private final List<GrizzlyAdapter> adapters = Lists.newArrayList();

  public void addGrizzlyAdapter(GrizzlyAdapter adapter) {
    adapters.add(adapter);
  }

  private final static String LOGGER_ATTR = "LOGGER_ATTR";
  
  @Override
  public void service(GrizzlyRequest request, GrizzlyResponse response) throws Exception {
    String context = request.getHeader(X_REQUEST_ID);
    if (context == null)
      context = "NoRequestId";
    TimingsLogger timingsLogger = timingsLoggerFactory.getLogger(context);
    timingsLogger.enterTimedArea();
    RequestScope.enter(request, timingsLogger);
    request.setAttribute(LOGGER_ATTR, timingsLogger);
    try {
      for (GrizzlyAdapter adapter : adapters) {
        try {
          request.setNote(INVOKED_ADAPTER_FLAG, adapter);
          adapter.service(request.getRequest(), response.getResponse());
          if (request.getRequest().getNote(ADAPTER_ABSTAINED_NOTE) == null)
            return;
        } catch (Exception e) {
          timingsLogger.setErrorState();
          throw e;
        } finally {
          request.getRequest().setNote(ADAPTER_ABSTAINED_NOTE, null);
        }
      }
    } finally {
      RequestScope.leave();
    }
    response.sendError(404, "No handler found");
  }

  @Override
  public void afterService(GrizzlyRequest request, GrizzlyResponse response) throws Exception {
    GrizzlyAdapter invokedAdapter = (GrizzlyAdapter) request.getNote(INVOKED_ADAPTER_FLAG);
    TimingsLogger timingsLogger = (TimingsLogger) request.getAttribute(LOGGER_ATTR);
    for (GrizzlyAdapter a : adapters) {
      try {
        a.afterService(request, response);
      } finally {
        request.removeNote(INVOKED_ADAPTER_FLAG);
      }
      if (a == invokedAdapter) {
        timingsLogger.leaveTimedArea();
        return;
      }
    }
  }
}
