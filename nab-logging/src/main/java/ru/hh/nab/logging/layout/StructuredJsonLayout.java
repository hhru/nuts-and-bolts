package ru.hh.nab.logging.layout;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.jackson.JacksonJsonFormatter;
import ch.qos.logback.contrib.json.JsonFormatter;
import ch.qos.logback.contrib.json.classic.JsonLayout;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static ru.hh.nab.logging.layout.StructuredJsonLayoutField.LEVEL;
import static ru.hh.nab.logging.layout.StructuredJsonLayoutField.MESSAGE;
import static ru.hh.nab.logging.layout.StructuredJsonLayoutField.THREAD;
import static ru.hh.nab.logging.layout.StructuredJsonLayoutField.TIMESTAMP;

public class StructuredJsonLayout extends JsonLayout {

  public static final String DEFAULT_TIMESTAMP_FORMAT = "yyyy-MM-dd' 'HH:mm:ss.SSSZ";

  private Boolean appendSeparator;

  @Override
  public void start() {
    if (getJsonFormatter() == null) {
      setJsonFormatter(createDefaultJsonFormatter());
    }
    if (getTimestampFormat() == null || getTimestampFormat().isEmpty()) {
      setTimestampFormat(DEFAULT_TIMESTAMP_FORMAT);
    }
    if (appendSeparator == null) {
      setAppendLineSeparator(true);
    }
    super.start();
  }

  @Override
  protected Map toJsonMap(ILoggingEvent event) {
    Map<String, String> mdc = new HashMap<>(event.getMDCPropertyMap());
    mdc.put(THREAD, event.getThreadName());

    Map<String, Object> map = new LinkedHashMap<>();
    addTimestamp(TIMESTAMP, true, event.getTimeStamp(), map);
    add(LEVEL, this.includeLevel, String.valueOf(event.getLevel()), map);
    add(JsonLayout.LOGGER_ATTR_NAME, this.includeLoggerName, event.getLoggerName(), map);
    addMap(JsonLayout.MDC_ATTR_NAME, this.includeMDC, mdc, map);
    add(MESSAGE, this.includeFormattedMessage, event.getFormattedMessage(), map);
    addThrowableInfo(JsonLayout.EXCEPTION_ATTR_NAME, this.includeException, event, map);
    return map;
  }

  @Override
  public void setAppendLineSeparator(boolean appendSeparator) {
    super.setAppendLineSeparator(appendSeparator);
    this.appendSeparator = appendSeparator;
  }

  private static JsonFormatter createDefaultJsonFormatter() {
    var formatter = new JacksonJsonFormatter();
    formatter.setPrettyPrint(false);
    return formatter;
  }
}
