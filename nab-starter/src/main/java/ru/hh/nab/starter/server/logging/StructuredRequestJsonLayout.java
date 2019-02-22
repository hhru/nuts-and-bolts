package ru.hh.nab.starter.server.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import ru.hh.nab.logging.layout.StructuredJsonLayout;

import static ru.hh.nab.logging.layout.StructuredJsonLayoutField.TIMESTAMP;
import static ru.hh.nab.starter.server.logging.StructuredRequestLogger.REQUEST_CACHE_MDC_KEY;
import static ru.hh.nab.starter.server.logging.StructuredRequestLogger.REQUEST_IP_MDC_KEY;
import static ru.hh.nab.starter.server.logging.StructuredRequestLogger.REQUEST_METHOD_MDC_KEY;
import static ru.hh.nab.starter.server.logging.StructuredRequestLogger.REQUEST_RID_MDC_KEY;
import static ru.hh.nab.starter.server.logging.StructuredRequestLogger.REQUEST_STATUS_MDC_KEY;
import static ru.hh.nab.starter.server.logging.StructuredRequestLogger.REQUEST_TIME_MDC_KEY;
import static ru.hh.nab.starter.server.logging.StructuredRequestLogger.REQUEST_URI_MDC_KEY;

public class StructuredRequestJsonLayout extends StructuredJsonLayout {

  public StructuredRequestJsonLayout() {
    super();
  }

  @Override
  protected Map toJsonMap(ILoggingEvent event) {
    Map<String, Object> map = new LinkedHashMap<>();
    addTimestamp(TIMESTAMP, this.includeTimestamp, event.getTimeStamp(), map);
    addCustomDataToJsonMap(map, event);
    return map;
  }

  @Override
  protected void addCustomDataToJsonMap(Map<String, Object> map, ILoggingEvent event) {
    Map<String, String> mdc = event.getMDCPropertyMap();
    map.put("ip", mdc.get(REQUEST_IP_MDC_KEY));
    map.put("rid", mdc.get(REQUEST_RID_MDC_KEY));
    map.put("status", Long.parseLong(mdc.get(REQUEST_STATUS_MDC_KEY)));
    map.put("cache", mdc.get(REQUEST_CACHE_MDC_KEY));
    map.put("time", Long.parseLong(mdc.get(REQUEST_TIME_MDC_KEY)));
    map.put("method", mdc.get(REQUEST_METHOD_MDC_KEY));
    map.put("uri", mdc.get(REQUEST_URI_MDC_KEY));
  }
}

