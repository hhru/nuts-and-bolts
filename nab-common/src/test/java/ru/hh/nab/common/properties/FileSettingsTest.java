package ru.hh.nab.common.properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

public class FileSettingsTest {
  private static FileSettings fileSettings;

  @BeforeClass
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

  @Test(expected = IllegalArgumentException.class)
  public void testGetSubPropertiesThrowsExceptionIfPrefixIsEmpty() {
    fileSettings.getSubProperties("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetSubPropertiesThrowsExceptionIfPrefixIsNull() {
    fileSettings.getSubProperties(null);
  }

  @Test
  public void testGetSubSettings() {
    FileSettings subSettings = fileSettings.getSubSettings("namespace");

    assertNotNull(subSettings.getString("boolProperty1"));
    assertNotNull(subSettings.getString("boolProperty2"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetSubSettingsThrowsExceptionIfPrefixIsEmpty() {
    fileSettings.getSubSettings("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetSubSettingsThrowsExceptionIfPrefixIsNull() {
    fileSettings.getSubSettings(null);
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
