package ru.hh.nab.grizzly;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.health.monitoring.TimingsLogger;
import ru.hh.health.monitoring.TimingsLoggerFactory;
import ru.hh.nab.grizzly.monitoring.MarkableProbe;
import ru.hh.nab.scopes.RequestScope;
import ru.hh.util.AcceptHeaderFixer;
import javax.ws.rs.WebApplicationException;
import java.util.List;
import java.util.concurrent.Callable;

public class SimpleGrizzlyAdapterChain extends HttpHandler {

  private static final String REQUEST_SERVICED = "GRIZZLY_ADAPTER_REQUEST_SERVICED";

  private final TimingsLoggerFactory timingsLoggerFactory;
  private final MarkableProbe[] probes;

  private final static String X_REQUEST_ID = "x-request-id";

  private final static Logger logger = LoggerFactory.getLogger(SimpleGrizzlyAdapterChain.class);

  public SimpleGrizzlyAdapterChain(TimingsLoggerFactory timingsLoggerFactory, MarkableProbe[] probes) {
    this.timingsLoggerFactory = timingsLoggerFactory;    
    this.probes = probes;
  }

  public SimpleGrizzlyAdapterChain(TimingsLoggerFactory timingsLoggerFactory) {
    this.timingsLoggerFactory = timingsLoggerFactory;
    this.probes = new MarkableProbe[0];
  }

  public static void requestServiced() {
    RequestScope.setProperty(REQUEST_SERVICED, REQUEST_SERVICED);
  }

  private final List<HttpHandler> adapters = Lists.newArrayList();

  public void addGrizzlyAdapter(HttpHandler adapter) {
    adapters.add(adapter);
  }

  @Override
  public void service(Request request, Response response) throws Exception {
    String fixedAcceptHeader = AcceptHeaderFixer.fixedAcceptHeaderOrNull(request.getHeader(Header.Accept));
    if (fixedAcceptHeader != null) {
      request.getRequest().setHeader(Header.Accept, fixedAcceptHeader);
    }

    String requestId = request.getHeader(X_REQUEST_ID);
    for (MarkableProbe probe : probes) {
      probe.mark(requestId, request.getRequest().getConnection().getPeerAddress().toString());
    }
    
    if (requestId == null)
      requestId = "NoRequestId";
    final TimingsLogger timingsLogger = timingsLoggerFactory.getLogger(
        String.format("%s %s", request.getMethod(), request.getRequestURI()),
        requestId
    );
    timingsLogger.enterTimedArea();
    RequestScope.enter(request, timingsLogger);
    RequestScope.addAfterServiceTask(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        timingsLogger.leaveTimedArea();
        return null;
      }
    });

    try {
      for (HttpHandler adapter : adapters) {
        try {
          adapter.service(request, response);
          if (StringUtils.isNotBlank(RequestScope.getProperty(REQUEST_SERVICED))) {
            RequestScope.removeProperty(REQUEST_SERVICED);
            return;
          }
        } catch (Exception e) {
          timingsLogger.setErrorState();
          final boolean doLogging;
          if (e instanceof WebApplicationException) {
            int status = ((WebApplicationException) e).getResponse().getStatus();
            doLogging = status >= 500;
          } else {
            doLogging = true;
          }
          if (doLogging) {
            timingsLogger.probe(e.getMessage());
            logger.error(e.getMessage(), e);
          }
          throw e;
        }
      }
    } finally {
      RequestScope.leave();
    }
    response.sendError(404, "No handler found");
  }
}
