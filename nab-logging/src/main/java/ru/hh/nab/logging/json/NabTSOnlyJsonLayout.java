package ru.hh.nab.logging.json;

import net.logstash.logback.layout.LogstashLayout;

import static ru.hh.nab.logging.json.JsonFieldNames.DEFAULT_TIMESTAMP_FORMAT;

public class NabTSOnlyJsonLayout extends LogstashLayout {

  public NabTSOnlyJsonLayout() {
    super();

    setFieldNames(LogstashFields.DEFAULT.getFieldNames());
    setIncludeMdc(false);
    setIncludeContext(false);
    setIncludeCallerData(false);
    setTimestampPattern(DEFAULT_TIMESTAMP_FORMAT);
  }
}
