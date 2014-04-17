package ru.hh.nab.jersey;

import com.sun.jersey.api.container.ContainerException;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.server.impl.ThreadLocalInvoker;
import com.sun.jersey.spi.container.ContainerListener;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseWriter;
import com.sun.jersey.spi.container.ReloadListener;
import com.sun.jersey.spi.container.WebApplication;
import com.sun.jersey.spi.inject.SingletonTypeInjectableProvider;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public final class JerseyHttpHandler extends HttpHandler implements ContainerListener {

  private final static Logger log = LoggerFactory.getLogger(JerseyHttpHandler.class);

  private WebApplication application;

  private final static String BASE_PATH = "/";

  private final ThreadLocalInvoker<Request> requestInvoker =
          new ThreadLocalInvoker<Request>();

  private final ThreadLocalInvoker<Response> responseInvoker =
          new ThreadLocalInvoker<Response>();

  private static class ContextInjectableProvider<T> extends
          SingletonTypeInjectableProvider<Context, T> {

    protected ContextInjectableProvider(Type type, T instance) {
      super(type, instance);
    }
  }

  public JerseyHttpHandler(ResourceConfig rc, WebApplication app) throws ContainerException {
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
  }

  private final static class Writer implements ContainerResponseWriter {
    final Response response;

    Writer(Response response) {
      this.response = response;
    }

    public OutputStream writeStatusAndHeaders(long contentLength,
                                              ContainerResponse cResponse) throws IOException {
      response.setStatus(cResponse.getStatus());

      if (contentLength != -1 && contentLength < Integer.MAX_VALUE)
        response.setContentLength((int) contentLength);

      for (Map.Entry<String, List<Object>> e : cResponse.getHttpHeaders().entrySet()) {
        for (Object value : e.getValue()) {
          response.addHeader(e.getKey(), ContainerResponse.getHeaderValue(value));
        }
      }

      String contentType = response.getHeader("Content-Type");
      if (contentType != null) {
        response.setContentType(contentType);
      }

      return response.getOutputStream();
    }

    public void finish() throws IOException {
    }
  }

  @Override
  public void service(Request request, Response response) {
    try {
      requestInvoker.set(request);
      responseInvoker.set(response);

      _service(request, response);
    } finally {
      requestInvoker.set(null);
      responseInvoker.set(null);
    }
  }

  private void _service(Request request, Response response) {
    WebApplication _application = application;

    // URI as provided by grizzly does not include query string
    final String requestUriString = request.getRequestURI() + '?' + request.getQueryString() ;
    final URI baseUri, resolvedRequestUri;

    try {
      baseUri = getBaseUri(request);
      resolvedRequestUri = baseUri.resolve(requestUriString);
    } catch (IllegalArgumentException ex) {
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

    try {
      final ContainerRequest cRequest = new ContainerRequest(
              _application,
              request.getMethod().getMethodString(),
              baseUri,
              resolvedRequestUri,
              getHeaders(request),
              request.getInputStream());
      _application.handleRequest(cRequest, new Writer(response));
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      try {
        response.sendError(500);
      } catch (IOException io) {
        throw new RuntimeException(io);
      }
    }
  }

  private URI getBaseUri(Request request) {
    try {
      return new URI(
        request.getScheme(),
        null,
        request.getServerName(),
        request.getServerPort(),
        BASE_PATH,
        null,
        null);
    } catch (URISyntaxException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  private InBoundHeaders getHeaders(Request request) {
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

    if (application.getFeaturesAndProperties() instanceof ReloadListener)
      ((ReloadListener) application.getFeaturesAndProperties()).onReload();

    oldApplication.destroy();
  }
}