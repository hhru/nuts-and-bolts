package ru.hh.nab.starter.server.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static ru.hh.nab.starter.server.logging.StructuredJsonLayoutField.LEVEL;
import static ru.hh.nab.starter.server.logging.StructuredJsonLayoutField.MESSAGE;
import static ru.hh.nab.starter.server.logging.StructuredJsonLayoutField.THREAD;
import static ru.hh.nab.starter.server.logging.StructuredJsonLayoutField.TIMESTAMP;

public class StructuredJsonLayout extends JsonLayout {

  @Override
  protected Map toJsonMap(ILoggingEvent event) {
    Map<String, String> mdc = new HashMap<>(event.getMDCPropertyMap());
    mdc.put(THREAD, event.getThreadName());

    Map<String, Object> map = new LinkedHashMap<>();
    addTimestamp(TIMESTAMP, true, event.getTimeStamp(), map);
    add(LEVEL, this.includeLevel, String.valueOf(event.getLevel()), map);
    add(LOGGER_ATTR_NAME, this.includeLoggerName, event.getLoggerName(), map);
    addMap(MDC_ATTR_NAME, this.includeMDC, mdc, map);
    add(MESSAGE, this.includeFormattedMessage, event.getFormattedMessage(), map);
    addThrowableInfo(EXCEPTION_ATTR_NAME, this.includeException, event, map);
    return map;
  }
}
