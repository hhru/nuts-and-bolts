package ru.hh.nab.web.jetty;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import static net.logstash.logback.marker.Markers.appendEntries;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.NanoTime;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import static ru.hh.jclient.common.HttpHeaderNames.X_OUTER_TIMEOUT_MS;
import static ru.hh.nab.common.constants.RequestAttributes.HTTP_ROUTE;
import static ru.hh.nab.common.constants.RequestHeaders.EMPTY_REQUEST_ID;
import static ru.hh.nab.common.constants.RequestHeaders.EMPTY_USER_AGENT;
import static ru.hh.nab.common.constants.RequestHeaders.REQUEST_ID;
import static ru.hh.nab.web.http.RequestInfo.CACHE_ATTRIBUTE;
import static ru.hh.nab.web.http.RequestInfo.NO_CACHE;

public class StructuredRequestLogger extends AbstractLifeCycle implements RequestLog {
  private static final Logger LOGGER = LoggerFactory.getLogger(StructuredRequestLogger.class);
  private static final Logger SLOW_REQUESTS = LoggerFactory.getLogger("slowRequests");

  private static final String UNKNOWN_URI = "unknown";

  @Override
  public void log(Request request, Response response) {
    final String outerTimoutMs = request.getHeaders().get(X_OUTER_TIMEOUT_MS);
    final String requestId = response.getHeaders().get(REQUEST_ID);
    final String userAgent = request.getHeaders().get(HttpHeaders.USER_AGENT);
    final String cache = (String) request.getAttribute(CACHE_ATTRIBUTE);

    Map<String, Object> context = new HashMap<>();
    context.put("ip", Request.getRemoteAddr(request));
    context.put("rid", ofNullable(requestId).orElse(EMPTY_REQUEST_ID));
    context.put("userAgent", ofNullable(userAgent).orElse(EMPTY_USER_AGENT));
    context.put("status", response.getStatus());
    context.put("cache", ofNullable(cache).orElse(NO_CACHE));
    long executionTime = NanoTime.millisSince(request.getHeadersNanoTime());
    context.put("time", executionTime);
    context.put("method", request.getMethod());
    context.put(
        "uri",
        Optional
            .ofNullable(request.getHttpURI())
            .map(HttpURI::getPathQuery)
            .orElse(UNKNOWN_URI)
    );
    context.put(HTTP_ROUTE, String.valueOf(request.getAttribute(HTTP_ROUTE)));

    LOGGER.info(appendEntries(context), null);
    ofNullable(outerTimoutMs)
        .map(Long::valueOf)
        .filter(timeoutMs -> timeoutMs > 0)
        .ifPresent(timeoutMs -> {
          if (executionTime > timeoutMs) {
            SLOW_REQUESTS.warn(appendEntries(context), null);
          }
        });
  }
}
