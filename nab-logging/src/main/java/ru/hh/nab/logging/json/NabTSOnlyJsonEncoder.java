package ru.hh.nab.logging.json;

import net.logstash.logback.encoder.LogstashEncoder;
import static ru.hh.nab.logging.json.JsonFieldNames.DEFAULT_TIMESTAMP_FORMAT;

public class NabTSOnlyJsonEncoder extends LogstashEncoder {
  public NabTSOnlyJsonEncoder() {
    this("undefined-appender-name", false);
  }

  public NabTSOnlyJsonEncoder(String appenderName, boolean includeAppenderName) {
    super();

    setFieldNames(LogstashFields.TS_ONLY.getFieldNames());
    setIncludeMdc(false);
    setIncludeContext(false);
    setIncludeCallerData(false);
    setTimestampPattern(DEFAULT_TIMESTAMP_FORMAT);
    if (includeAppenderName) {
      addProvider(new AppenderNameJsonProvider(appenderName));
    }
  }
}
