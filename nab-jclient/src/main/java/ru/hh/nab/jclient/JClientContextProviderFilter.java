package ru.hh.nab.jclient;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Spliterators;
import java.util.stream.StreamSupport;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.HttpClientContextThreadLocalSupplier;
import ru.hh.nab.common.component.NabServletFilter;
import ru.hh.nab.common.servlet.UriComponent;
import static java.util.Objects.requireNonNull;
import static java.util.Spliterator.DISTINCT;
import static java.util.Spliterator.NONNULL;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class JClientContextProviderFilter implements Filter, NabServletFilter {
  private static final Logger LOGGER = LoggerFactory.getLogger(JClientContextProviderFilter.class);
  private static final String USER_AGENT ="User-Agent";
  private final HttpClientContextThreadLocalSupplier contextThreadLocalSupplier;
  private final TelemetryPropagator telemetryPropagator;
  private final Tracer tracer;


  public JClientContextProviderFilter(HttpClientContextThreadLocalSupplier contextThreadLocalSupplier, Tracer tracer,
                                      TelemetryPropagator telemetryPropagator
  ) {
    this.contextThreadLocalSupplier = contextThreadLocalSupplier;
    this.telemetryPropagator = telemetryPropagator;
    this.tracer = tracer;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    requireNonNull(contextThreadLocalSupplier, "httpClientContextSupplier should not be null");
    Map<String, List<String>> requestHeadersMap = getRequestHeadersMap(request);
//    ((org.eclipse.jetty.server.Request) request).getHttpURI().getPath()  //todo воткнуть нормальный пас. Из сервлета ничего не достать..
    Span span = startSpan(request.getServerName(), requestHeadersMap, telemetryPropagator);
    try (Scope scope = span.makeCurrent()) {
      try {
        contextThreadLocalSupplier.addContext(requestHeadersMap, getQueryParamsMap(request));
      } catch (IllegalArgumentException e) {
        ((HttpServletResponse) response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      chain.doFilter(request, response);
    } finally {
      span.end();
      contextThreadLocalSupplier.clear();
    }
  }

  private Span startSpan(String path, Map<String, List<String>> requestHeadersMap, TelemetryPropagator telemetryPropagator) {
    Context telemetryContext = telemetryPropagator.getTelemetryContext(Context.current(), requestHeadersMap);
    Span span = tracer.spanBuilder(path)
        .setParent(telemetryContext)
        .setSpanKind(SpanKind.SERVER)
        .setAttribute(USER_AGENT, requestHeadersMap.get(USER_AGENT) == null ? "unknown" : requestHeadersMap.get(USER_AGENT).get(0))
        .startSpan();
    LOGGER.trace("span started:{}", span);
    return span;
  }

  private static Map<String, List<String>> getRequestHeadersMap(ServletRequest req) {
    HttpServletRequest request = (HttpServletRequest) req;
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(request.getHeaderNames().asIterator(), DISTINCT | NONNULL), false)
      .collect(toMap(identity(), h -> List.of(request.getHeader(h))));
  }

  private static Map<String, List<String>> getQueryParamsMap(ServletRequest req) {
    HttpServletRequest request = (HttpServletRequest) req;
    return UriComponent.decodeQuery(request.getQueryString(), true, true);
  }
}
