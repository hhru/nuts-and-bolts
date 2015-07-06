package ru.hh.nab.grizzly;

import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import ru.hh.nab.scopes.RequestScope;

import static ru.hh.nab.grizzly.SimpleGrizzlyAdapterChain.setResponseInTimings;

public abstract class RequestHandler {

  public abstract void handle(Request request, Response response) throws Exception;

  protected void suspend() {
    RequestScope.incrementAfterServiceLatchCounter();
    ((Request) RequestScope.currentRequest()).getResponse().suspend();
  }

  protected void resumeWithRedirect(String url) {
    final Response response = ((Request) RequestScope.currentRequest()).getResponse();
    response.setHeader("Location", url);
    resumeWithStatus(302);
  }

  protected void resumeTemporaryUnavailable() {
    resumeWithStatus(503);
  }

  protected void resumeOk() {
    resumeWithStatus(200);
  }

  protected void resumeWithStatus(int code) {
    final Response response = ((Request) RequestScope.currentRequest()).getResponse();
    response.setStatus(code);
    setResponseInTimings(RequestScope.currentTimingsLogger(), response);
    resume();
  }

  protected void resume() {
    ((Request) RequestScope.currentRequest()).getResponse().resume();
    RequestScope.decrementAfterServiceLatchCounter();
  }
}
