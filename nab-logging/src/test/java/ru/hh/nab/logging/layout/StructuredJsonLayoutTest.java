package ru.hh.nab.logging.layout;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;

@SuppressWarnings("rawtypes")
public class StructuredJsonLayoutTest {

  @Test
  public void testToJsonMapEmptyMap() {
    StructuredJsonLayout layout = new StructuredJsonLayout();
    LoggingEvent eventObject = new LoggingEvent();
    eventObject.setCallerData(new StackTraceElement[] {new StackTraceElement("class", "method", null, 1)});
    eventObject.setMDCPropertyMap(Collections.emptyMap());
    eventObject.setLevel(Level.ERROR);
    eventObject.setTimeStamp(0);
    eventObject.setMessage("message");
    eventObject.setLoggerName("logger");
    Map map = layout.toJsonMap(eventObject);
    assertFalse(map.isEmpty());
  }

  @Test
  public void testToJsonMapSingletonMap() {
    StructuredJsonLayout layout = new StructuredJsonLayout();
    LoggingEvent eventObject = new LoggingEvent();
    eventObject.setCallerData(new StackTraceElement[] {new StackTraceElement("class", "method", null, 1)});
    eventObject.setMDCPropertyMap(Collections.singletonMap("test", "entry"));
    eventObject.setLevel(Level.ERROR);
    eventObject.setTimeStamp(0);
    eventObject.setMessage("message");
    eventObject.setLoggerName("logger");
    Map map = layout.toJsonMap(eventObject);
    assertFalse(map.isEmpty());
  }

  @Test
  public void testToJsonMapHashMap() {
    StructuredJsonLayout layout = new StructuredJsonLayout();
    LoggingEvent eventObject = new LoggingEvent();
    eventObject.setCallerData(new StackTraceElement[] {new StackTraceElement("class", "method", null, 1)});
    eventObject.setMDCPropertyMap(new HashMap<>());
    eventObject.setLevel(Level.ERROR);
    eventObject.setTimeStamp(0);
    eventObject.setMessage("message");
    eventObject.setLoggerName("logger");
    Map map = layout.toJsonMap(eventObject);
    assertFalse(map.isEmpty());
  }
}
