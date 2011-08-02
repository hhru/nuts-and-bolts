package ru.hh.nab.grizzly;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.sun.grizzly.tcp.http11.GrizzlyRequest;
import com.sun.grizzly.tcp.http11.GrizzlyResponse;
import java.util.Set;
import java.util.concurrent.Semaphore;

public class HandlerDecorator implements RequestHandler {

  private final RequestHandler target;
  private final Set<HttpMethod> methods;
  private final Semaphore semaphore;

  public HandlerDecorator(RequestHandler target, HttpMethod[] methods, int concurrency) {
    Preconditions.checkArgument(concurrency >= 0);
    this.target = target;
    this.methods = ImmutableSet.copyOf(methods);
    this.semaphore = (concurrency > 0) ?  new Semaphore(concurrency) : null;
  }

  public boolean tryBegin() {
    if (semaphore == null)
      return true;
    return semaphore.tryAcquire();
  }

  public void finish() {
    if (semaphore != null)
      semaphore.release();
  }

  @Override
  public void handle(GrizzlyRequest request, GrizzlyResponse response) throws Exception {
    if (methods.contains(HttpMethod.valueOf(request.getRequest().method().getString())))
      target.handle(request, response);
    else
      response.sendError(405);
  }
}
