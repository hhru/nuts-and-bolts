package ru.hh.nab.logging.json;

import net.logstash.logback.layout.LogstashLayout;
import static ru.hh.nab.logging.json.JsonFieldNames.DEFAULT_TIMESTAMP_FORMAT;

public class NabJsonLayout extends LogstashLayout {
  public NabJsonLayout() {
    super();

    setFieldNames(LogstashFields.DEFAULT.getFieldNames());
    setIncludeMdc(true);
    setIncludeContext(false);
    setIncludeCallerData(false);
    setTimestampPattern(DEFAULT_TIMESTAMP_FORMAT);
  }
}
