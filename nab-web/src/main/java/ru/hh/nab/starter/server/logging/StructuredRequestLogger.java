package ru.hh.nab.starter.server.logging;

import static java.lang.System.currentTimeMillis;
import java.util.HashMap;
import java.util.Map;
import static java.util.Optional.ofNullable;
import static net.logstash.logback.marker.Markers.appendEntries;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import static ru.hh.jclient.common.HttpHeaderNames.X_OUTER_TIMEOUT_MS;
import static ru.hh.nab.starter.server.RequestHeaders.EMPTY_REQUEST_ID;
import static ru.hh.nab.starter.server.RequestHeaders.EMPTY_USER_AGENT;
import static ru.hh.nab.starter.server.RequestHeaders.REQUEST_ID;
import static ru.hh.nab.starter.server.logging.RequestInfo.CACHE_ATTRIBUTE;
import static ru.hh.nab.starter.server.logging.RequestInfo.NO_CACHE;

public class StructuredRequestLogger extends AbstractLifeCycle implements RequestLog {
  private static final Logger LOGGER = LoggerFactory.getLogger(StructuredRequestLogger.class);
  private static final Logger SLOW_REQUESTS = LoggerFactory.getLogger("slowRequests");

  @Override
  public void log(Request request, Response response) {
    final String outerTimoutMs = request.getHeader(X_OUTER_TIMEOUT_MS);
    final String requestId = request.getHeader(REQUEST_ID);
    final String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
    final String cache = (String) request.getAttribute(CACHE_ATTRIBUTE);

    Map<String, Object> context = new HashMap<>();
    context.put("ip", request.getRemoteHost());
    context.put("rid", ofNullable(requestId).orElse(EMPTY_REQUEST_ID));
    context.put("userAgent", ofNullable(userAgent).orElse(EMPTY_USER_AGENT));
    context.put("status", response.getCommittedMetaData().getStatus());
    context.put("cache", ofNullable(cache).orElse(NO_CACHE));
    long executionTime = currentTimeMillis() - request.getTimeStamp();
    context.put("time", executionTime);
    context.put("method", request.getMethod());
    context.put("uri", request.getHttpURI().getPathQuery());

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
