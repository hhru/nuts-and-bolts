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
import javax.inject.Provider;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.health.monitoring.TimingsLogger;
import ru.hh.nab.grizzly.SimpleGrizzlyAdapterChain;
import ru.hh.nab.scopes.RequestScope;
import ru.hh.util.UriTool;
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

  private static final Logger log = LoggerFactory.getLogger(JerseyHttpHandler.class);

  private static final String GRIZZLY_IO_EXCEPTION_DURING_JERSEY_CALL = "GRIZZLY_IO_EXCEPTION_DURING_JERSEY_CALL";

  private WebApplication application;

  private final static String BASE_PATH = "/";

  private final ThreadLocalInvoker<Request> requestInvoker =
          new ThreadLocalInvoker<Request>();

  private final ThreadLocalInvoker<Response> responseInvoker =
          new ThreadLocalInvoker<Response>();

  private final Provider<TimingsLogger> timingsLoggerProvider;


  private static class ContextInjectableProvider<T> extends
          SingletonTypeInjectableProvider<Context, T> {

    protected ContextInjectableProvider(Type type, T instance) {
      super(type, instance);
    }
  }

  public JerseyHttpHandler(ResourceConfig rc, WebApplication app, Provider<TimingsLogger> timingsLoggerProvider) throws ContainerException {
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
  }

  private final static class Writer implements ContainerResponseWriter {
    final Response response;

    Writer(Response response) {
      this.response = response;
    }

    public OutputStream writeStatusAndHeaders(long contentLength,
                                              ContainerResponse cResponse) throws IOException {
      response.setStatus(cResponse.getStatus());

      if (contentLength != -1 && contentLength < Integer.MAX_VALUE) {
        response.setContentLength((int) contentLength);
      }

      for (Map.Entry<String, List<Object>> e : cResponse.getHttpHeaders().entrySet()) {
        for (Object value : e.getValue()) {
          response.addHeader(e.getKey(), ContainerResponse.getHeaderValue(value));
        }
      }

      String contentType = response.getHeader("Content-Type");
      if (contentType != null) {
        response.setContentType(contentType);
      }

      return new OutputStreamRememberingIOExceptions(response.getOutputStream());
    }

    public void finish() throws IOException {
    }
  }

  private static void rememberIOException(IOException exception) {
    RequestScope.setNamedObject(IOException.class, GRIZZLY_IO_EXCEPTION_DURING_JERSEY_CALL, exception);
  }

  private static IOException recallIOException() {
    return RequestScope.getNamedObject(IOException.class, GRIZZLY_IO_EXCEPTION_DURING_JERSEY_CALL);
  }

  private static class OutputStreamRememberingIOExceptions extends OutputStream {
    final OutputStream delegate;

    public OutputStreamRememberingIOExceptions(OutputStream os) {
      this.delegate = os;
    }

    @Override
    public void write(int b) throws IOException {
      try {
        delegate.write(b);
      } catch (IOException ex) {
        rememberIOException(ex);
        throw ex;
      }
    }

    @Override
    public void write(byte[] b) throws IOException {
      try {
        delegate.write(b);
      } catch (IOException ex) {
        rememberIOException(ex);
        throw ex;
      }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      try {
        delegate.write(b, off, len);
      } catch (IOException ex) {
        rememberIOException(ex);
        throw ex;
      }
    }

    @Override
    public void flush() throws IOException {
      try {
        delegate.flush();
      } catch (IOException ex) {
        rememberIOException(ex);
        throw ex;
      }
    }

    @Override
    public void close() throws IOException {
      try {
        delegate.close();
      } catch (IOException ex) {
        rememberIOException(ex);
        throw ex;
      }
    }
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
    WebApplication _application = application;

    // URI as provided by grizzly does not include query string
    final String requestQueryString = request.getQueryString();
    final String requestUriString = requestQueryString == null
        ? request.getRequestURI()
        : request.getRequestURI() + '?' + requestQueryString;
    final URI baseUri, resolvedRequestUri;

    try {
      baseUri = getBaseUri(request);
      resolvedRequestUri = baseUri.resolve(UriTool.getUri(requestUriString));
    } catch (Exception ex) {
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
      Writer writer = new Writer(response);
      timingsLogger().probe("jersey#beforeHandle");
      _application.handleRequest(cRequest, writer);
      timingsLogger().probe("jersey#afterHandle");

      if (response.getStatus() >= 500) {
        timingsLogger().setErrorState();
      }
    } catch (IOException | RuntimeException e) {
      // Jersey throws WebApplicationException (extends RuntimeException)
      // when response is committed and might throw some other RuntimeException
      // in the future, so we are catching both IOException and RuntimeException
      // to reduce dependency on jersey internal implementation.
      IOException recalledException = recallIOException();
      if (recalledException == null) {
        // this is not a grizzly i/o error, rethrow
        throw e;
      }
      TimingsLogger timingsLogger = RequestScope.currentTimingsLogger();
      timingsLogger.setErrorState();
      timingsLogger.probe(e.getMessage());
      log.warn(recalledException.getMessage(), recalledException);
    }
    SimpleGrizzlyAdapterChain.requestServiced();
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

    if (application.getFeaturesAndProperties() instanceof ReloadListener) {
      ((ReloadListener) application.getFeaturesAndProperties()).onReload();
    }

    oldApplication.destroy();
  }

  private TimingsLogger timingsLogger() {
    return timingsLoggerProvider.get();
  }
}
