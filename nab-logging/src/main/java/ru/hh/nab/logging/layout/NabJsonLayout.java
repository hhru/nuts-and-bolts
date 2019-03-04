
package ru.hh.nab.logging.layout;

import net.logstash.logback.layout.LogstashLayout;

import static ru.hh.nab.logging.layout.LogstashFields.DEFAULT_TIMESTAMP_FORMAT;
import static ru.hh.nab.logging.layout.LogstashFields.FIELD_NAMES;

public class NabJsonLayout extends LogstashLayout {
  public NabJsonLayout() {
    super();

    setFieldNames(FIELD_NAMES);
    setIncludeMdc(true);
    setIncludeContext(false);
    setIncludeCallerData(false);
    setTimestampPattern(DEFAULT_TIMESTAMP_FORMAT);
  }
}
