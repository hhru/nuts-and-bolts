package ru.hh.nab.jetty;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.health.monitoring.TimingsLogger;
import ru.hh.health.monitoring.TimingsLoggerFactory;
import ru.hh.nab.jersey.JerseyHttpServletAdapter;
import ru.hh.nab.scopes.RequestScope;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class JettyRequestHandler extends AbstractHandler {

  private final TimingsLoggerFactory timingsLoggerFactory;
  private final JerseyHttpServletAdapter adapter;

  private final static String X_REQUEST_ID = "x-request-id";

  private final static Logger logger = LoggerFactory.getLogger(JettyRequestHandler.class);
  private static final String NO_REQUEST_ID = "NoRequestId";

  public JettyRequestHandler(TimingsLoggerFactory timingsLoggerFactory,
                             JerseyHttpServletAdapter adapter) {
    this.timingsLoggerFactory = timingsLoggerFactory;
    this.adapter = adapter;
  }

  @Override
  public void handle(
    String target,
    Request baseRequest,
    HttpServletRequest request,
    HttpServletResponse response) throws IOException {
    response.setHeader("X-Accel-Buffering", "no");
    final String requestId = request.getHeader(X_REQUEST_ID) == null ? NO_REQUEST_ID : request.getHeader(X_REQUEST_ID);
    final TimingsLogger timingsLogger = timingsLoggerFactory.getLogger(
      String.format("%s %s", request.getMethod(), request.getRequestURI()),
      requestId
    );
    timingsLogger.enterTimedArea();

    RequestScope.enter(request, timingsLogger);
    RequestScope.addAfterServiceTask(() -> {
        timingsLogger.leaveTimedArea();
        return null;
      }
    );

    try {
      adapter.service(request, response);
    } catch (Exception e) {
      timingsLogger.setErrorState();
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
