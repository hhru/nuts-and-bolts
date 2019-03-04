package ru.hh.nab.logging.layout;

import net.logstash.logback.fieldnames.LogstashFieldNames;

import static net.logstash.logback.fieldnames.LogstashCommonFieldNames.IGNORE_FIELD_INDICATOR;

public final class LogstashFields {
  public static final String TIMESTAMP = "ts";
  public static final String LEVEL = "lvl";
  public static final String MESSAGE = "msg";
  public static final String LOGGER = "logger";
  public static final String EXCEPTION = "exception";
  public static final String MDC = "mdc";

  public static final String DEFAULT_TIMESTAMP_FORMAT = "yyyy-MM-dd' 'HH:mm:ss.SSSZ";

  static final LogstashFieldNames FIELD_NAMES = new LogstashFieldNames();
  static {
    FIELD_NAMES.setTimestamp(TIMESTAMP);
    FIELD_NAMES.setVersion(IGNORE_FIELD_INDICATOR);
    FIELD_NAMES.setMessage(MESSAGE);
    FIELD_NAMES.setLogger(LOGGER);
    FIELD_NAMES.setThread(IGNORE_FIELD_INDICATOR);
    FIELD_NAMES.setLevel(LEVEL);
    FIELD_NAMES.setLevelValue(IGNORE_FIELD_INDICATOR);
    FIELD_NAMES.setStackTrace(EXCEPTION);
    FIELD_NAMES.setMdc(MDC);
  }

  static final LogstashFieldNames FIELD_NAMES_TS_ONLY = new LogstashFieldNames();
  static {
    FIELD_NAMES_TS_ONLY.setTimestamp(TIMESTAMP);
    FIELD_NAMES_TS_ONLY.setVersion(IGNORE_FIELD_INDICATOR);
    FIELD_NAMES_TS_ONLY.setMessage(IGNORE_FIELD_INDICATOR);
    FIELD_NAMES_TS_ONLY.setLogger(IGNORE_FIELD_INDICATOR);
    FIELD_NAMES_TS_ONLY.setThread(IGNORE_FIELD_INDICATOR);
    FIELD_NAMES_TS_ONLY.setLevel(IGNORE_FIELD_INDICATOR);
    FIELD_NAMES_TS_ONLY.setLevelValue(IGNORE_FIELD_INDICATOR);
    FIELD_NAMES_TS_ONLY.setStackTrace(IGNORE_FIELD_INDICATOR);
    FIELD_NAMES_TS_ONLY.setMdc(IGNORE_FIELD_INDICATOR);
  }
}
