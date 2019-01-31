package ru.hh.nab.starter.server.logging;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static java.lang.System.currentTimeMillis;
import static ru.hh.nab.starter.server.logging.RequestInfo.CACHE_ATTRIBUTE;
import static ru.hh.nab.starter.server.logging.RequestInfo.EMPTY_REQUEST_ID;
import static ru.hh.nab.starter.server.logging.RequestInfo.NO_CACHE;
import static ru.hh.nab.starter.server.logging.RequestInfo.REQUEST_ID_HEADER;

public class StructuredRequestLogger extends AbstractLifeCycle implements RequestLog {
  private static final Logger log = LoggerFactory.getLogger(StructuredRequestLogger.class);

  static String REQUEST_IP_MDC_KEY = "r_ip";
  static String REQUEST_RID_MDC_KEY = "r_rid";
  static String REQUEST_STATUS_MDC_KEY = "r_status";
  static String REQUEST_CACHE_MDC_KEY = "r_cache";
  static String REQUEST_TIME_MDC_KEY = "r_time";
  static String REQUEST_METHOD_MDC_KEY = "r_method";
  static String REQUEST_URI_MDC_KEY = "r_uri";

  @Override
  public void log(Request request, Response response) {
    try {
      final String requestId = request.getHeader(REQUEST_ID_HEADER);
      final String cache = (String) request.getAttribute(CACHE_ATTRIBUTE);
      MDC.put(REQUEST_IP_MDC_KEY, request.getRemoteHost());
      MDC.put(REQUEST_RID_MDC_KEY, requestId == null ? EMPTY_REQUEST_ID : requestId);
      MDC.put(REQUEST_STATUS_MDC_KEY, String.valueOf(response.getCommittedMetaData().getStatus()));
      MDC.put(REQUEST_CACHE_MDC_KEY, cache == null ? NO_CACHE : cache);
      MDC.put(REQUEST_TIME_MDC_KEY, String.valueOf(currentTimeMillis() - request.getTimeStamp()));
      MDC.put(REQUEST_METHOD_MDC_KEY, request.getMethod());
      MDC.put(REQUEST_URI_MDC_KEY, request.getHttpURI().getPathQuery());

      log.info(null);
    } finally {
      MDC.remove(REQUEST_IP_MDC_KEY);
      MDC.remove(REQUEST_RID_MDC_KEY);
      MDC.remove(REQUEST_STATUS_MDC_KEY);
      MDC.remove(REQUEST_CACHE_MDC_KEY);
      MDC.remove(REQUEST_TIME_MDC_KEY);
      MDC.remove(REQUEST_METHOD_MDC_KEY);
      MDC.remove(REQUEST_URI_MDC_KEY);
    }
  }
}

