package ru.hh.nab.logging.json;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import net.logstash.logback.composite.AbstractFieldJsonProvider;
import static ru.hh.nab.logging.json.JsonFieldNames.APPENDER;

/**
 * JsonProvider для добавления имени appender'а в JSON лог
 */
public class AppenderNameJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> {

  private String appenderName;

  public AppenderNameJsonProvider(String appenderName) {
    this.appenderName = appenderName;
    setFieldName(APPENDER);
  }

  @Override
  public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
    if (appenderName != null && !appenderName.isEmpty()) {
      generator.writeStringField(getFieldName(), appenderName);
    }
  }
}
