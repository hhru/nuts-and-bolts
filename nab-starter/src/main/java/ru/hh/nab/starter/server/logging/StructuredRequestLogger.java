package ru.hh.nab.starter.server.logging;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static java.lang.System.currentTimeMillis;
import static net.logstash.logback.marker.Markers.appendEntries;
import static ru.hh.nab.starter.server.logging.RequestInfo.CACHE_ATTRIBUTE;
import static ru.hh.nab.starter.server.logging.RequestInfo.EMPTY_REQUEST_ID;
import static ru.hh.nab.starter.server.logging.RequestInfo.NO_CACHE;
import static ru.hh.nab.starter.server.logging.RequestInfo.REQUEST_ID_HEADER;

public class StructuredRequestLogger extends AbstractLifeCycle implements RequestLog {
  private static final Logger LOGGER = LoggerFactory.getLogger(StructuredRequestLogger.class);

  @Override
  public void log(Request request, Response response) {
    final String requestId = request.getHeader(REQUEST_ID_HEADER);
    final String cache = (String) request.getAttribute(CACHE_ATTRIBUTE);

    Map<String, Object> context = new HashMap<>();
    context.put("ip", request.getRemoteHost());
    context.put("rid", requestId == null ? EMPTY_REQUEST_ID : requestId);
    context.put("status", response.getCommittedMetaData().getStatus());
    context.put("cache", cache == null ? NO_CACHE : cache);
    context.put("time", currentTimeMillis() - request.getTimeStamp());
    context.put("method", request.getMethod());
    context.put("uri", request.getHttpURI().getPathQuery());

    LOGGER.info(appendEntries(context), null);
  }
}
