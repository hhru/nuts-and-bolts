package ru.hh.nab.grizzly;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.health.monitoring.TimingsLogger;
import ru.hh.health.monitoring.TimingsLoggerFactory;
import ru.hh.nab.grizzly.monitoring.ConnectionProbeTimingLogger;
import ru.hh.nab.scopes.RequestScope;
import ru.hh.util.AcceptHeaderFixer;
import java.util.List;
import java.util.concurrent.Callable;

public class SimpleGrizzlyAdapterChain extends HttpHandler {

  private static final String REQUEST_SERVICED = "GRIZZLY_ADAPTER_REQUEST_SERVICED";

  private final TimingsLoggerFactory timingsLoggerFactory;
  private final ConnectionProbeTimingLogger probe;

  private final static String X_REQUEST_ID = "x-request-id";

  private final static Logger logger = LoggerFactory.getLogger(SimpleGrizzlyAdapterChain.class);
  private String NO_REQUEST_ID = "NoRequestId";

  public SimpleGrizzlyAdapterChain(TimingsLoggerFactory timingsLoggerFactory, ConnectionProbeTimingLogger probe) {
    this.timingsLoggerFactory = timingsLoggerFactory;    
    this.probe = probe;
  }

  public SimpleGrizzlyAdapterChain(TimingsLoggerFactory timingsLoggerFactory) {
    this.timingsLoggerFactory = timingsLoggerFactory;
    this.probe = null;
  }

  public static void requestServiced() {
    RequestScope.setProperty(REQUEST_SERVICED, REQUEST_SERVICED);
  }

  private final List<HttpHandler> adapters = Lists.newArrayList();

  public void addGrizzlyAdapter(HttpHandler adapter) {
    adapters.add(adapter);
  }

  @Override
  public void service(final Request request, final Response response) throws Exception {
    String fixedAcceptHeader = AcceptHeaderFixer.fixedAcceptHeaderOrNull(request.getHeader(Header.Accept));
    if (fixedAcceptHeader != null) {
      request.getRequest().setHeader(Header.Accept, fixedAcceptHeader);
    }
    final String requestId = request.getHeader(X_REQUEST_ID) == null ? NO_REQUEST_ID : request.getHeader(X_REQUEST_ID);
    final TimingsLogger timingsLogger = timingsLoggerFactory.getLogger(
        String.format("%s %s", request.getMethod(), request.getRequestURI()),
        requestId
    );
    timingsLogger.enterTimedArea();
    RequestScope.enter(request, timingsLogger);
    final Connection connection = request.getRequest().getConnection();
    RequestScope.addAfterServiceTask(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        timingsLogger.leaveTimedArea();
        if (probe != null && !NO_REQUEST_ID.equals(requestId)) {
          probe.endUserRequest(requestId, connection);
        }
        return null;
      }
    });

    try {
      for (HttpHandler adapter : adapters) {
        adapter.service(request, response);
        if (StringUtils.isNotBlank(RequestScope.getProperty(REQUEST_SERVICED))) {
          RequestScope.removeProperty(REQUEST_SERVICED);
          return;
        }
      }
      response.sendError(404, "No handler found");
    } catch (Exception e) {
      timingsLogger.setErrorState();
      timingsLogger.probe(e.getMessage());
      logger.error(e.getMessage(), e);

      // send error response if response if not already committed
      if (!response.isCommitted()) {
        response.sendError(500, e.getMessage());
      }
    } finally {
      RequestScope.leave();
    }
  }
}
