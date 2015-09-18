package ru.hh.nab.jersey;

import com.sun.jersey.api.container.ContainerException;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.server.impl.ThreadLocalInvoker;
import com.sun.jersey.spi.container.ContainerListener;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ReloadListener;
import com.sun.jersey.spi.container.WebApplication;
import com.sun.jersey.spi.inject.SingletonTypeInjectableProvider;
import javax.inject.Provider;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.health.monitoring.TimingsLogger;
import ru.hh.nab.grizzly.SimpleGrizzlyAdapterChain;
import ru.hh.util.UriTool;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;

public final class JerseyHttpHandler extends HttpHandler implements ContainerListener {

  private static final Logger log = LoggerFactory.getLogger(JerseyHttpHandler.class);

  private WebApplication application;

  private final static String BASE_PATH = "/";

  private final ThreadLocalInvoker<Request> requestInvoker =
          new ThreadLocalInvoker<Request>();

  private final ThreadLocalInvoker<Response> responseInvoker =
          new ThreadLocalInvoker<Response>();

  private final Provider<TimingsLogger> timingsLoggerProvider;

  private final boolean allowFlush;

  private static class ContextInjectableProvider<T> extends
          SingletonTypeInjectableProvider<Context, T> {

    protected ContextInjectableProvider(Type type, T instance) {
      super(type, instance);
    }
  }

  public JerseyHttpHandler(final ResourceConfig rc,
                           final WebApplication app,
                           final Provider<TimingsLogger> timingsLoggerProvider,
                           final boolean allowFlush) throws ContainerException {
    this.application = app;

    GenericEntity<ThreadLocal<Request>> requestThreadLocal =
            new GenericEntity<ThreadLocal<Request>>(requestInvoker.getImmutableThreadLocal()) {
            };
    rc.getSingletons().add(new ContextInjectableProvider<ThreadLocal<Request>>(
            requestThreadLocal.getType(), requestThreadLocal.getEntity()));

    GenericEntity<ThreadLocal<Response>> responseThreadLocal =
            new GenericEntity<ThreadLocal<Response>>(responseInvoker.getImmutableThreadLocal()) {
            };
    rc.getSingletons().add(new ContextInjectableProvider<ThreadLocal<Response>>(
            responseThreadLocal.getType(), responseThreadLocal.getEntity()));
    this.timingsLoggerProvider = timingsLoggerProvider;
    this.allowFlush = allowFlush;
  }

  @Override
  public void service(Request request, Response response) throws IOException {
    try {
      requestInvoker.set(request);
      responseInvoker.set(response);

      internalService(request, response);
    } finally {
      requestInvoker.set(null);
      responseInvoker.set(null);
    }
  }

  private void internalService(Request request, Response response) throws IOException {
    // this is used to handle onReload properly
    WebApplication app = application;

    // URI as provided by grizzly does not include query string
    final String requestQueryString = request.getQueryString();
    final String requestUriString = requestQueryString == null
        ? request.getRequestURI()
        : request.getRequestURI() + '?' + requestQueryString;
    final URI baseUri, resolvedRequestUri;

    try {
      baseUri = getBaseUri(request);
      resolvedRequestUri = baseUri.resolve(UriTool.getUri(requestUriString));
    } catch (URISyntaxException ex) {
      if (log.isDebugEnabled()) {
        log.warn(String.format("Could not resolve URI %s, producing HTTP 400", requestUriString), ex);
      } else {
        log.warn("Could not resolve URI {}, producing HTTP 400", requestUriString);
      }
      response.setStatus(400);
      return;
    }

    /**
     * Check if the request URI path starts with the base URI path
     */
    if (!resolvedRequestUri.getRawPath().startsWith(BASE_PATH)) {
      response.setStatus(404);
      return;
    }

    final ContainerRequest cRequest = new ContainerRequest(
      app,
      request.getMethod().getMethodString(),
      baseUri,
      resolvedRequestUri,
      getHeaders(request),
      request.getInputStream());
    final JerseyToGrizzlyResponseWriter responseWriter =
      new JerseyToGrizzlyResponseWriter(response, allowFlush);
    timingsLogger().probe("jersey#beforeHandle");
    app.handleRequest(cRequest, responseWriter);
    timingsLogger().probe("jersey#afterHandle");

    final Exception ioException = responseWriter.getException();
    if (ioException != null) {
      TimingsLogger timingsLogger = timingsLogger();
      timingsLogger.setErrorState();
      timingsLogger.probe(ioException.getMessage());
      log.warn(ioException.getMessage(), ioException);
    } else if (response.getStatus() >= 500) {
      timingsLogger().setErrorState();
    }
    SimpleGrizzlyAdapterChain.requestServiced();
  }

  private static URI getBaseUri(Request request) throws URISyntaxException {
    return new URI(
      request.getScheme(),
      null,
      request.getServerName(),
      request.getServerPort(),
      BASE_PATH,
      null,
      null);
  }

  private static InBoundHeaders getHeaders(Request request) {
    InBoundHeaders rh = new InBoundHeaders();

    for (String name : request.getHeaderNames()) {
      String value = request.getHeader(name);
      rh.add(name, value);
    }

    return rh;
  }

  // ContainerListener

  public void onReload() {
    WebApplication oldApplication = application;
    application = application.clone();

    if (application.getFeaturesAndProperties() instanceof ReloadListener) {
      ((ReloadListener) application.getFeaturesAndProperties()).onReload();
    }

    oldApplication.destroy();
  }

  private TimingsLogger timingsLogger() {
    return timingsLoggerProvider.get();
  }
}
