package ru.hh.nab.grizzly;

import com.google.common.collect.ImmutableSet;
import com.sun.grizzly.tcp.http11.GrizzlyRequest;
import com.sun.grizzly.tcp.http11.GrizzlyResponse;

import java.util.Set;

public class RequestAdapter implements RequestHandler {

  private final RequestHandler target;
  private final Set<HttpMethod> methods;

  public RequestAdapter(RequestHandler target, HttpMethod[] methods) {
    this.target = target;
    this.methods = ImmutableSet.copyOf(methods);
  }

  @Override
  public void handle(GrizzlyRequest request, GrizzlyResponse response) throws Exception {
    if (methods.contains(HttpMethod.valueOf(request.getRequest().method().getString())))
      target.handle(request, response);
    else
      response.sendError(405);
  }
}
