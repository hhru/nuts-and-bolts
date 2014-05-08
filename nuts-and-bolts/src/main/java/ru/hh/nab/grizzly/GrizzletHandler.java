package ru.hh.nab.grizzly;

import com.google.common.collect.ImmutableSet;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.health.limits.LeaseToken;
import ru.hh.nab.health.limits.Limit;
import ru.hh.nab.health.limits.Limits;
import ru.hh.nab.scopes.RequestScope;
import java.util.Set;
import java.util.concurrent.Callable;

public class GrizzletHandler {
  private final String path;
  private final RequestHandler target;
  private final Set<HttpMethod> methods;
  private final Limit limit;
  private final static Logger LOGGER = LoggerFactory.getLogger(GrizzletHandler.class);

  public GrizzletHandler(Class<? extends RequestHandler> handlerClass, RequestHandler target, Limits limits) {
    this.path = extractPath(handlerClass);
    if (path.charAt(0) != '/') {
      throw new IllegalArgumentException("Path must start with '/'");
    }
    this.target = target;
    this.methods = ImmutableSet.copyOf(extractMethods(handlerClass));
    this.limit = limits.compoundLimit(extractConcurrency(handlerClass));
  }

  public String getPath() {
    return path;
  }

  public Set<HttpMethod> getMethods() {
    return methods;
  }

  public void handle(Request request, Response response) throws Exception {
    final LeaseToken leaseToken = limit.acquire();
    if (leaseToken == null) {
      LOGGER.warn("Failed to acquire limit, too many requests, responding with 503");
      response.sendError(503);
    } else {
      RequestScope.addAfterServiceTask(new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          leaseToken.release();
          return null;
        }
      });
      target.handle(request, response);
    }
  }

  private static Route maybeGetRoute(Class<? extends RequestHandler> handler) {
    Route route = handler.getAnnotation(Route.class);
    if (route == null) {
      throw new IllegalArgumentException("@Route in " + handler.getSimpleName() + "?");
    }
    return route;
  }

  private static HttpMethod[] extractMethods(Class<? extends RequestHandler> handler) {
    return maybeGetRoute(handler).methods();
  }

  private static String extractPath(Class<? extends RequestHandler> handler) {
    return maybeGetRoute(handler).path();
  }

  private static String[] extractConcurrency(Class<? extends RequestHandler> handler) {
    Concurrency concurrency = handler.getAnnotation(Concurrency.class);
    if (concurrency == null) {
      return new String[] { "global" };
    }
    return concurrency.value();
  }
}
