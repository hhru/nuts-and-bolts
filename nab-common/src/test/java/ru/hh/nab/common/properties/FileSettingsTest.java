package ru.hh.nab.common.properties;

import java.util.List;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class FileSettingsTest {
  private static FileSettings fileSettings;

  @BeforeAll
  public static void setUpClass() {
    Properties properties = new Properties();
    properties.put("strProperty", "value");
    properties.put("intProperty1", "0");
    properties.put("intProperty2", "123");
    properties.put("namespace.boolProperty1", "true");
    properties.put("namespace.boolProperty2", "false");
    properties.put("listProperty", "value1, value2,value3");

    fileSettings = new FileSettings(properties);
  }

  @Test
  public void testGetString() {
    assertEquals("value", fileSettings.getString("strProperty"));
    assertNull(fileSettings.getString("missingKey"));
  }

  @Test
  public void testGetInteger() {
    assertEquals(0, fileSettings.getInteger("intProperty1").intValue());
    assertEquals(123, fileSettings.getInteger("intProperty2").intValue());
  }

  @Test
  public void testGetLong() {
    assertEquals(0L, fileSettings.getLong("intProperty1").longValue());
    assertEquals(123L, fileSettings.getLong("intProperty2").longValue());
  }

  @Test
  public void testGetBoolean() {
    assertTrue(fileSettings.getBoolean("namespace.boolProperty1"));
    assertFalse(fileSettings.getBoolean("namespace.boolProperty2"));
  }

  @Test
  public void testGetSubProperties() {
    Properties subProperties = fileSettings.getSubProperties("namespace");

    assertEquals(2, subProperties.size());
    assertTrue(subProperties.containsKey("boolProperty1"));
    assertTrue(subProperties.containsKey("boolProperty2"));
  }

  @Test
  public void testGetSubPropertiesThrowsExceptionIfPrefixIsEmpty() {
    assertThrows(IllegalArgumentException.class, () -> fileSettings.getSubProperties(""));
  }

  @Test
  public void testGetSubPropertiesThrowsExceptionIfPrefixIsNull() {
    assertThrows(IllegalArgumentException.class, () -> fileSettings.getSubProperties(null));
  }

  @Test
  public void testGetSubSettings() {
    FileSettings subSettings = fileSettings.getSubSettings("namespace");

    assertNotNull(subSettings.getString("boolProperty1"));
    assertNotNull(subSettings.getString("boolProperty2"));
  }

  @Test
  public void testGetSubSettingsThrowsExceptionIfPrefixIsEmpty() {
    assertThrows(IllegalArgumentException.class, () -> fileSettings.getSubSettings(""));
  }

  @Test
  public void testGetSubSettingsThrowsExceptionIfPrefixIsNull() {
    assertThrows(IllegalArgumentException.class, () -> fileSettings.getSubSettings(null));
  }

  @Test
  public void testGetStringList() {
    List<String> settings = fileSettings.getStringList("listProperty");

    assertEquals(3, settings.size());
    assertEquals("value1", settings.get(0));
    assertEquals("value2", settings.get(1));
    assertEquals("value3", settings.get(2));
  }

  @Test
  public void testGetProperties() {
    Properties properties = fileSettings.getProperties();
    assertEquals("value", properties.getProperty("strProperty"));

    properties.setProperty("strProperty", "newValue");
    assertEquals("newValue", properties.getProperty("strProperty"));
    assertEquals("value", fileSettings.getString("strProperty"));
  }
}
