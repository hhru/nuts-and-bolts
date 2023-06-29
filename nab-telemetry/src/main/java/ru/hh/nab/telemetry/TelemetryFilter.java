package ru.hh.nab.telemetry;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import static java.lang.String.CASE_INSENSITIVE_ORDER;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.common.component.NabServletFilter;
import static ru.hh.nab.common.mdc.MDC.CONTROLLER_MDC_KEY;

public class TelemetryFilter implements Filter, NabServletFilter {
  private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryFilter.class);
  public static final String STATUS_URL = "/status";

  private final TelemetryPropagator telemetryPropagator;
  private final Tracer tracer;
  private final boolean enabled;

  public TelemetryFilter(Tracer tracer, TelemetryPropagator telemetryPropagator, boolean enabled) {
    this.telemetryPropagator = telemetryPropagator;
    this.tracer = tracer;
    this.enabled = enabled;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpServletRequest = (HttpServletRequest) request;
    URL url = getFullUrl(httpServletRequest);

    if (!enabled || url == null || STATUS_URL.equals(url.getPath())) {
      chain.doFilter(request, response);
    } else {
      Map<String, String> requestHeadersMap = getRequestHeadersMap(request);
      Context telemetryContext = telemetryPropagator.getTelemetryContext(Context.current(), requestHeadersMap);

      Span span = tracer.spanBuilder("unknown controller")
          .setParent(telemetryContext)
          .setSpanKind(SpanKind.SERVER)
          .setAttribute(SemanticAttributes.HTTP_METHOD, httpServletRequest.getMethod())
          .setAttribute(SemanticAttributes.HTTP_HOST, url.getHost())
          .setAttribute(SemanticAttributes.HTTP_TARGET, url.getFile())
          .setAttribute(SemanticAttributes.HTTP_SCHEME, url.getProtocol())
          .setAttribute(SemanticAttributes.HTTP_CLIENT_IP, request.getRemoteAddr())
          .startSpan();
      LOGGER.trace("span started:{}", span);

      try (Scope ignored = span.makeCurrent()) {
        chain.doFilter(request, response);
        String controller = (String) httpServletRequest.getAttribute(CONTROLLER_MDC_KEY);
        if (controller != null) {
          span.updateName(controller);
        }
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        span.setAttribute(SemanticAttributes.HTTP_STATUS_CODE, httpServletResponse.getStatus());
        span.setStatus(TelemetryPropagator.getStatus(httpServletResponse.getStatus(), true));
      } finally {
        span.end();
      }
    }
  }

  private static URL getFullUrl(HttpServletRequest request) {
    StringBuffer url = request.getRequestURL();
    String queryString = request.getQueryString();
    if (queryString != null) {
      url.append('?').append(queryString);
    }

    try {
      return new URL(url.toString());
    } catch (MalformedURLException e) {
      LOGGER.error("failed to parse request url", e);
      return null;
    }
  }

  private static Map<String, String> getRequestHeadersMap(ServletRequest req) {
    HttpServletRequest request = (HttpServletRequest) req;
    Enumeration<String> names = request.getHeaderNames();

    if (names == null) {
      return Map.of();
    }

    TreeMap<String, String> headers = new TreeMap<>(CASE_INSENSITIVE_ORDER);

    while (names.hasMoreElements()) {
      String name = names.nextElement();
      headers.put(name, request.getHeader(name));
    }

    return headers;
  }
}
