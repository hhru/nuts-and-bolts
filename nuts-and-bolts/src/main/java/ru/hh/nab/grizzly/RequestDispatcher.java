package ru.hh.nab.grizzly;

import com.sun.grizzly.tcp.http11.GrizzlyAdapter;
import com.sun.grizzly.tcp.http11.GrizzlyRequest;
import com.sun.grizzly.tcp.http11.GrizzlyResponse;

public class RequestDispatcher extends GrizzlyAdapter {

  private final Router router;

  public RequestDispatcher(Router router) {
    this.router = router;
  }

  private final String HANDLER_ATTR = "handler-attr";

  @Override
  public void service(GrizzlyRequest request, GrizzlyResponse response) throws Exception {
    String uri = request.getRequestURI();
    uri = PathUtil.removeLastSlash(uri);
    String path = uri.substring(PathUtil.contextPath(uri).length());
    HandlerDecorator handler = router.route(path);
    if (handler == null) {
      response.sendError(404);
    } else {
      if (!handler.tryBegin()) {
        response.sendError(503);
      } else {
        request.setAttribute(HANDLER_ATTR, handler);
        handler.handle(request, response);
      }
    }
  }

  @Override
  public void afterService(GrizzlyRequest request, GrizzlyResponse response) throws Exception {
    HandlerDecorator handler = (HandlerDecorator) request.getAttribute(HANDLER_ATTR);
    if (handler != null)
      handler.finish();
  }
}

