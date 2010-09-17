package ru.hh.nab.jersey;

import com.sun.grizzly.tcp.http11.GrizzlyAdapter;
import com.sun.grizzly.tcp.http11.GrizzlyRequest;
import com.sun.grizzly.tcp.http11.GrizzlyResponse;
import com.sun.jersey.api.container.ContainerException;
import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.spi.container.ContainerListener;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseWriter;
import com.sun.jersey.spi.container.WebApplication;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public final class NabGrizzlyContainer extends GrizzlyAdapter implements ContainerListener {

  private WebApplication application;
  static final ThreadLocal<GrizzlyRequest> CURRENT_REQUEST = new ThreadLocal<GrizzlyRequest>();

  public NabGrizzlyContainer(WebApplication app) throws ContainerException {
    this.application = app;
  }

  private final static class Writer implements ContainerResponseWriter {
    final GrizzlyResponse response;

    Writer(GrizzlyResponse response) {
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

  public void service(GrizzlyRequest request, GrizzlyResponse response) {
    WebApplication _application = application;

    final URI baseUri = getBaseUri(request);
    /*
    * request.unparsedURI() is a URI in encoded form that contains
    * the URI path and URI query components.
    */
    final URI requestUri = baseUri.resolve(
            request.getRequest().unparsedURI().toString());

    try {
      final ContainerRequest cRequest = new ContainerRequest(
              _application,
              request.getMethod(),
              baseUri,
              requestUri,
              getHeaders(request),
              request.getInputStream());

      CURRENT_REQUEST.set(request);
      _application.handleRequest(cRequest, new Writer(response));
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    } finally {
      CURRENT_REQUEST.remove();
    }
  }

  public void afterService(GrizzlyRequest request, GrizzlyResponse response)
          throws Exception {
  }

  private URI getBaseUri(GrizzlyRequest request) {
    try {
      return new URI(
              request.getScheme(),
              null,
              request.getServerName(),
              request.getServerPort(),
              "/",
              null, null);
    } catch (URISyntaxException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  private InBoundHeaders getHeaders(GrizzlyRequest request) {
    InBoundHeaders rh = new InBoundHeaders();

    Enumeration names = request.getHeaderNames();
    while (names.hasMoreElements()) {
      String name = (String) names.nextElement();
      String value = request.getHeader(name);
      rh.add(name, value);
    }

    return rh;
  }

  // ContainerListener

  public void onReload() {
    WebApplication oldApplication = application;
    application = application.clone();
    oldApplication.destroy();
  }
}