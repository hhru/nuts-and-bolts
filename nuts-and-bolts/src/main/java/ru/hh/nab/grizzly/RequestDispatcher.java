package ru.hh.nab.grizzly;

import com.sun.grizzly.tcp.http11.GrizzlyAdapter;
import com.sun.grizzly.tcp.http11.GrizzlyRequest;
import com.sun.grizzly.tcp.http11.GrizzlyResponse;

public class RequestDispatcher extends GrizzlyAdapter {

  private final Router router;

  public RequestDispatcher(Router router) {
    this.router = router;
  }

  @Override
  public void service(GrizzlyRequest request, GrizzlyResponse response) throws Exception {
    String uri = request.getRequestURI();
    uri = PathUtil.removeLastSlash(uri);
    String path = uri.substring(PathUtil.contextPath(uri).length());
    RequestHandler handler = router.route(path);
    if (handler == null)
      response.sendError(404);
    else
      handler.handle(request, response);
  }
}

