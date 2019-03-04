package ru.hh.nab.logging.layout;

import net.logstash.logback.encoder.LogstashEncoder;

import static ru.hh.nab.logging.layout.LogstashFields.DEFAULT_TIMESTAMP_FORMAT;
import static ru.hh.nab.logging.layout.LogstashFields.FIELD_NAMES_TS_ONLY;

public class NabTSOnlyJsonEncoder extends LogstashEncoder {
  public NabTSOnlyJsonEncoder() {
    super();

    setFieldNames(FIELD_NAMES_TS_ONLY);
    setIncludeMdc(false);
    setIncludeContext(false);
    setIncludeCallerData(false);
    setTimestampPattern(DEFAULT_TIMESTAMP_FORMAT);
  }
}
