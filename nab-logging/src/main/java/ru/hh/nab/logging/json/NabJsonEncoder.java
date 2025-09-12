package ru.hh.nab.logging.json;

import net.logstash.logback.encoder.LogstashEncoder;
import static ru.hh.nab.logging.json.JsonFieldNames.DEFAULT_TIMESTAMP_FORMAT;

public class NabJsonEncoder extends LogstashEncoder {

  public NabJsonEncoder() {
    this("undefined-appender-name", false);
  }

  public NabJsonEncoder(String appenderName, boolean includeAppenderName) {
    super();

    setFieldNames(LogstashFields.DEFAULT.getFieldNames());
    setIncludeMdc(true);
    setIncludeContext(false);
    setIncludeCallerData(false);
    setTimestampPattern(DEFAULT_TIMESTAMP_FORMAT);
    setLineSeparator("\n");

    if (includeAppenderName) {
      addProvider(new AppenderNameJsonProvider(appenderName));
    }
  }
}
