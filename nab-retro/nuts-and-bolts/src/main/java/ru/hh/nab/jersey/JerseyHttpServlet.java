package ru.hh.nab.jersey;

import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.WebApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.health.monitoring.TimingsLogger;
import ru.hh.nab.scopes.RequestScope;
import ru.hh.util.UriTool;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Enumeration;

public final class JerseyHttpServlet extends HttpServlet {

  private static final Logger log = LoggerFactory.getLogger(JerseyHttpServlet.class);

  private final WebApplication app;

  public JerseyHttpServlet(final WebApplication app) {
    this.app = app;
  }

  @Override
  protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {

    request.setCharacterEncoding(Charset.defaultCharset().name());

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

    final ContainerRequest cRequest = new ContainerRequest(
      app,
      request.getMethod(),
      baseUri,
      resolvedRequestUri,
      getHeaders(request),
      request.getInputStream());
    final JerseyToHttpServletResponseWriter responseWriter =
      new JerseyToHttpServletResponseWriter(request, response);
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
  }

  private static URI getBaseUri(HttpServletRequest request) throws URISyntaxException {
    return new URI(
      request.getScheme(),
      null,
      request.getServerName(),
      request.getServerPort(),
      "/",
      null,
      null);
  }

  // convert headers, also replace accept header with wildcard if contains wildcard because
  // Jersey can not parse malformed Accept header sent by various mobile browsers
  private static InBoundHeaders getHeaders(HttpServletRequest request) {
    InBoundHeaders rh = new InBoundHeaders();

    for (Enumeration<String> names = request.getHeaderNames(); names.hasMoreElements();) {
      final String name = names.nextElement();
      for (Enumeration<String> values = request.getHeaders(name); values.hasMoreElements();) {
        final String value = values.nextElement();
        if (name.equalsIgnoreCase(HttpHeaders.ACCEPT) && value.contains(MediaType.WILDCARD)) {
          rh.add(HttpHeaders.ACCEPT, MediaType.WILDCARD);
          break;
        }
        rh.add(name, value);
      }
    }

    return rh;
  }

  private TimingsLogger timingsLogger() {
    return RequestScope.currentTimingsLogger();
  }
}
