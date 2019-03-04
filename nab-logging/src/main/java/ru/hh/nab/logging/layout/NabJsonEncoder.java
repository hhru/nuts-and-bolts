package ru.hh.nab.logging.layout;

import net.logstash.logback.encoder.LogstashEncoder;

import static ru.hh.nab.logging.layout.LogstashFields.DEFAULT_TIMESTAMP_FORMAT;
import static ru.hh.nab.logging.layout.LogstashFields.FIELD_NAMES;

public class NabJsonEncoder extends LogstashEncoder {
  public NabJsonEncoder() {
    super();

    setFieldNames(FIELD_NAMES);
    setIncludeMdc(true);
    setIncludeContext(false);
    setIncludeCallerData(false);
    setTimestampPattern(DEFAULT_TIMESTAMP_FORMAT);
    setLineSeparator("\n");
  }
}
