package ru.hh.nab.telemetry;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.semconv.ClientAttributes;
import io.opentelemetry.semconv.CodeAttributes;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import io.opentelemetry.semconv.UserAgentAttributes;
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
import static java.util.Optional.ofNullable;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static ru.hh.nab.common.constants.RequestAttributes.CODE_FUNCTION_NAME;
import static ru.hh.nab.common.constants.RequestAttributes.HTTP_ROUTE;
import ru.hh.nab.common.constants.RequestHeaders;
import static ru.hh.nab.common.mdc.MDC.CONTROLLER_MDC_KEY;
import ru.hh.nab.telemetry.semconv.SemanticAttributesForRemoval;
import ru.hh.trace.TraceContextUnsafe;
import ru.hh.trace.TraceIdGenerator;

public class TelemetryFilter implements Filter {
  private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryFilter.class);
  public static final String STATUS_URL = "/status";

  private final TelemetryPropagator telemetryPropagator;
  private final Tracer tracer;
  private final TraceContextUnsafe traceContext;

  public TelemetryFilter(Tracer tracer, TelemetryPropagator telemetryPropagator, TraceContextUnsafe traceContext) {
    this.telemetryPropagator = telemetryPropagator;
    this.tracer = tracer;
    this.traceContext = traceContext;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpServletRequest = (HttpServletRequest) request;
    HttpServletResponse httpServletResponse = (HttpServletResponse) response;
    URL url = getFullUrl(httpServletRequest);

    if (url == null || STATUS_URL.equals(url.getPath())) {
      String traceId = httpServletRequest.getHeader(RequestHeaders.REQUEST_ID);
      httpServletResponse.addHeader(RequestHeaders.REQUEST_ID, traceId == null ? RequestHeaders.EMPTY_REQUEST_ID : traceId);
      chain.doFilter(request, response);
    } else {
      Map<String, String> requestHeadersMap = getRequestHeadersMap(httpServletRequest);
      Context telemetryContext = telemetryPropagator.getTelemetryContext(Context.current(), requestHeadersMap);

      String traceId = ofNullable(requestHeadersMap.get(RequestHeaders.REQUEST_ID)).orElseGet(TraceIdGenerator::generateTraceId);
      String strictTraceId = Span.fromContext(telemetryContext).getSpanContext().getTraceId();
      try (ru.hh.trace.Scope ignoredTraceContextScope = traceContext.setTraceIdWithStrictTraceIdCorrection(traceId, strictTraceId)) {
        //noinspection OptionalGetWithoutIsPresent
        httpServletResponse.addHeader(RequestHeaders.REQUEST_ID, traceContext.getTraceId().get());

        SpanBuilder spanBuilder = tracer
            .spanBuilder("unknown controller")
            .setParent(telemetryContext)
            .setSpanKind(SpanKind.SERVER)

            .setAttribute(SemanticAttributesForRemoval.HTTP_METHOD, httpServletRequest.getMethod())
            .setAttribute(HttpAttributes.HTTP_REQUEST_METHOD, httpServletRequest.getMethod())

            .setAttribute(SemanticAttributesForRemoval.HTTP_HOST, url.getHost())
            .setAttribute(ServerAttributes.SERVER_ADDRESS, url.getHost())

            .setAttribute(SemanticAttributesForRemoval.HTTP_TARGET, url.getFile())
            .setAttribute(UrlAttributes.URL_PATH, url.getPath());
        if (url.getQuery() != null) {
          spanBuilder.setAttribute(UrlAttributes.URL_QUERY, url.getQuery());
        }

        Span span = spanBuilder
            .setAttribute(SemanticAttributesForRemoval.HTTP_SCHEME, url.getProtocol())
            .setAttribute(UrlAttributes.URL_SCHEME, url.getProtocol())

            .setAttribute(SemanticAttributesForRemoval.HTTP_CLIENT_IP, request.getRemoteAddr())
            .setAttribute(ClientAttributes.CLIENT_ADDRESS, request.getRemoteAddr())

            .setAttribute(UserAgentAttributes.USER_AGENT_ORIGINAL, ofNullable(httpServletRequest.getHeader("User-Agent")).orElse("noUserAgent"))
            .startSpan();

        LOGGER.trace("span started:{}", span);

        try (Scope ignored = span.makeCurrent()) {
          chain.doFilter(request, response);
          String controller = (String) httpServletRequest.getAttribute(CONTROLLER_MDC_KEY);
          if (controller != null) {
            span.updateName(controller);
          }
          String codeFunctionName = (String) httpServletRequest.getAttribute(CODE_FUNCTION_NAME);
          if (codeFunctionName != null) {
            span.setAttribute(CodeAttributes.CODE_FUNCTION_NAME, codeFunctionName);

            int lastDotIndex = codeFunctionName.lastIndexOf('.');
            String codeNamespace = codeFunctionName.substring(0, lastDotIndex);
            String codeFunction = codeFunctionName.substring(lastDotIndex + 1);

            span.setAttribute(SemanticAttributesForRemoval.CODE_FUNCTION, codeFunction);
            span.setAttribute(SemanticAttributesForRemoval.CODE_NAMESPACE, codeNamespace);
          }
          String httpRoute = (String) httpServletRequest.getAttribute(HTTP_ROUTE);
          if (httpRoute != null) {
            span.setAttribute(HttpAttributes.HTTP_ROUTE, httpRoute);
          }

          span.setAttribute(SemanticAttributesForRemoval.HTTP_STATUS_CODE, httpServletResponse.getStatus());
          span.setAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, httpServletResponse.getStatus());
          span.setStatus(TelemetryPropagator.getStatus(httpServletResponse.getStatus(), true));
        } finally {
          span.end();
        }
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

  private static Map<String, String> getRequestHeadersMap(HttpServletRequest request) {
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
