package ru.hh.nab.logging.json;

import net.logstash.logback.fieldnames.LogstashFieldNames;

import static net.logstash.logback.fieldnames.LogstashCommonFieldNames.IGNORE_FIELD_INDICATOR;
import static ru.hh.nab.logging.json.JsonFieldNames.EXCEPTION;
import static ru.hh.nab.logging.json.JsonFieldNames.LEVEL;
import static ru.hh.nab.logging.json.JsonFieldNames.LOGGER;
import static ru.hh.nab.logging.json.JsonFieldNames.MDC;
import static ru.hh.nab.logging.json.JsonFieldNames.MESSAGE;
import static ru.hh.nab.logging.json.JsonFieldNames.TIMESTAMP;

public enum LogstashFields {
  DEFAULT(new LogstashFieldNames()),
  TS_ONLY(new LogstashFieldNames());

  private LogstashFieldNames fieldNames;

  static {
    DEFAULT.fieldNames.setTimestamp(TIMESTAMP);
    DEFAULT.fieldNames.setVersion(IGNORE_FIELD_INDICATOR);
    DEFAULT.fieldNames.setMessage(MESSAGE);
    DEFAULT.fieldNames.setLogger(LOGGER);
    DEFAULT.fieldNames.setThread(IGNORE_FIELD_INDICATOR);
    DEFAULT.fieldNames.setLevel(LEVEL);
    DEFAULT.fieldNames.setLevelValue(IGNORE_FIELD_INDICATOR);
    DEFAULT.fieldNames.setStackTrace(EXCEPTION);
    DEFAULT.fieldNames.setMdc(MDC);
  }

  static {
    TS_ONLY.fieldNames.setTimestamp(TIMESTAMP);
    TS_ONLY.fieldNames.setVersion(IGNORE_FIELD_INDICATOR);
    TS_ONLY.fieldNames.setMessage(IGNORE_FIELD_INDICATOR);
    TS_ONLY.fieldNames.setLogger(IGNORE_FIELD_INDICATOR);
    TS_ONLY.fieldNames.setThread(IGNORE_FIELD_INDICATOR);
    TS_ONLY.fieldNames.setLevel(IGNORE_FIELD_INDICATOR);
    TS_ONLY.fieldNames.setLevelValue(IGNORE_FIELD_INDICATOR);
    TS_ONLY.fieldNames.setStackTrace(IGNORE_FIELD_INDICATOR);
    TS_ONLY.fieldNames.setMdc(IGNORE_FIELD_INDICATOR);
  }

  LogstashFields(LogstashFieldNames fieldNames) {
    this.fieldNames = fieldNames;
  }

  public LogstashFieldNames getFieldNames() {
    return fieldNames;
  }
}
