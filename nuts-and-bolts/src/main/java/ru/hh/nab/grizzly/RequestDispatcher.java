package ru.hh.nab.grizzly;

import com.sun.grizzly.tcp.http11.GrizzlyAdapter;
import com.sun.grizzly.tcp.http11.GrizzlyRequest;
import com.sun.grizzly.tcp.http11.GrizzlyResponse;
import ru.hh.nab.health.limits.LeaseToken;

public class RequestDispatcher extends GrizzlyAdapter {

  private final Router router;

  public RequestDispatcher(Router router) {
    this.router = router;
  }

  private final String LEASE_TOKEN_ATTR = "lease-token";

  @Override
  public void service(GrizzlyRequest request, GrizzlyResponse response) throws Exception {
    String uri = request.getRequestURI();
    HandlerDecorator handler = router.route(uri);
    if (handler == null) {
      SimpleGrizzlyAdapterChain.abstain(request);
    } else {
      LeaseToken leaseToken = handler.tryBegin();
      if (leaseToken == null) {
        response.sendError(503);
      } else {
        request.setAttribute(LEASE_TOKEN_ATTR, leaseToken);
        handler.handle(request, response);
      }
    }
  }

  @Override
  public void afterService(GrizzlyRequest request, GrizzlyResponse response) throws Exception {
    LeaseToken leaseToken = (LeaseToken) request.getAttribute(LEASE_TOKEN_ATTR);
    if (leaseToken != null)
      leaseToken.release();
  }
}

