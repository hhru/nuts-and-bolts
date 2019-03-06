package ru.hh.nab.logging.json;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.hh.nab.logging.json.JsonFieldNames.LEVEL;
import static ru.hh.nab.logging.json.JsonFieldNames.LOGGER;
import static ru.hh.nab.logging.json.JsonFieldNames.MDC;
import static ru.hh.nab.logging.json.JsonFieldNames.MESSAGE;

public class NabJsonLayoutTest {
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final NabJsonLayout layout = new NabJsonLayout();
  private static final NabJsonEncoder encoder = new NabJsonEncoder();
  static {
    layout.start();
    encoder.start();
  }

  @Test
  public void testJsonEmptyMdc() throws IOException {
    LoggingEvent event = new LoggingEvent();
    event.setCallerData(new StackTraceElement[] {new StackTraceElement("class", "method", null, 1)});
    event.setMDCPropertyMap(Collections.emptyMap());
    event.setLevel(Level.ERROR);
    event.setTimeStamp(0);
    event.setMessage("message");
    event.setLoggerName("logger");

    Map<String, String> eventJson = objectMapper.readValue(layout.doLayout(event), new TypeReference<Map<String, String>>(){});

    assertFalse(eventJson.isEmpty());
    assertEquals("ERROR", eventJson.get(LEVEL));
    assertEquals("message", eventJson.get(MESSAGE));
    assertEquals("logger", eventJson.get(LOGGER));
    assertNull(eventJson.get(MDC));
  }

  @Test
  public void testJsonEncoder() throws IOException {
    LoggingEvent event = new LoggingEvent();
    event.setCallerData(new StackTraceElement[] {new StackTraceElement("class", "method", null, 1)});
    event.setMDCPropertyMap(Collections.emptyMap());
    event.setLevel(Level.ERROR);
    event.setTimeStamp(0);
    event.setMessage("message");
    event.setLoggerName("logger");

    String jsonString = new String(encoder.encode(event));
    Map<String, String> eventJson = objectMapper.readValue(jsonString, new TypeReference<Map<String, String>>(){});

    assertFalse(eventJson.isEmpty());
    assertTrue(jsonString.endsWith("\n"));
    assertEquals("ERROR", eventJson.get(LEVEL));
    assertEquals("message", eventJson.get(MESSAGE));
    assertEquals("logger", eventJson.get(LOGGER));
    assertNull(eventJson.get(MDC));
  }

  @Test
  public void testJsonWithMdc() throws IOException {
    LoggingEvent event = new LoggingEvent();
    event.setCallerData(new StackTraceElement[] {new StackTraceElement("class", "method", null, 1)});
    event.setMDCPropertyMap(Collections.singletonMap("test", "entry"));
    event.setLevel(Level.ERROR);
    event.setTimeStamp(0);
    event.setMessage("message");
    event.setLoggerName("logger");

    JsonNode eventJson = objectMapper.readValue(layout.doLayout(event), JsonNode.class);
    List<String> mdcKeys = new ArrayList<>();
    eventJson.path(MDC).fieldNames().forEachRemaining(mdcKeys::add);

    assertEquals(List.of("test"), mdcKeys);
    assertEquals("entry", eventJson.path(MDC).path("test").asText());
  }
}
