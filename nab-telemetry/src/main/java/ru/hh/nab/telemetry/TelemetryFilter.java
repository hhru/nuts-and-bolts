package ru.hh.nab.telemetry;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import static java.util.Spliterator.DISTINCT;
import static java.util.Spliterator.NONNULL;
import java.util.Spliterators;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import java.util.stream.StreamSupport;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.common.component.NabServletFilter;

public class TelemetryFilter implements Filter, NabServletFilter {
  private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryFilter.class);
  private static final String USER_AGENT = "User-Agent";
  private static final String SERVER_URL = "Server-url";
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
    if (!enabled) {
      return;
    }
    Map<String, List<String>> requestHeadersMap = getRequestHeadersMap(request);
    Context telemetryContext = telemetryPropagator.getTelemetryContext(Context.current(), requestHeadersMap);
    Span span = tracer.spanBuilder(request.getServerName())
        .setParent(telemetryContext)
        .setSpanKind(SpanKind.SERVER)
        .setAttribute(USER_AGENT, requestHeadersMap.get(USER_AGENT) == null ? "unknown" : requestHeadersMap.get(USER_AGENT).get(0))
        .setAttribute(SERVER_URL, ((HttpServletRequest) request).getRequestURL().toString())
        .startSpan();
    LOGGER.trace("span started:{}", span);

    try (Scope scope = span.makeCurrent()) {
      chain.doFilter(request, response);
    } finally {
      span.end();
    }
  }

  private static Map<String, List<String>> getRequestHeadersMap(ServletRequest req) {
    HttpServletRequest request = (HttpServletRequest) req;
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(request.getHeaderNames().asIterator(), DISTINCT | NONNULL), false)
        .collect(toMap(identity(), h -> List.of(request.getHeader(h))));
  }
}
