package ru.hh.nab.common.properties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static ru.hh.nab.common.properties.PropertiesUtils.DEFAULT_DEV_FILE_EXT;
import static ru.hh.nab.common.properties.PropertiesUtils.SETTINGS_DIR_PROPERTY;
import static ru.hh.nab.common.properties.PropertiesUtils.fromFilesInSettingsDir;

public class PropertiesUtilsTest {
  private static final String TMP_DIR = System.getProperty("java.io.tmpdir");
  private static final String TEST_FILE_PREFIX = "settings.properties";
  private static Properties testProperties;
  private Path propertiesFile;
  private Path devPropertiesFile;

  @BeforeAll
  public static void setUpClass() {
    testProperties = new Properties();
    testProperties.put("stringProperty", "value");
    testProperties.put("emptyProperty", "");
    testProperties.put("intProperty", Integer.toString(Integer.MAX_VALUE));
    testProperties.put("longProperty", Long.toString(Long.MAX_VALUE));
    testProperties.put("doubleProperty", Double.toString(Double.MAX_VALUE));
    testProperties.put("boolProperty", Boolean.TRUE.toString());
    testProperties.put("listProperty", "value1, value2,value3");
    testProperties.put("namespace.property1", "value1");
    testProperties.put("namespace.property2", "value2");
  }

  @BeforeEach
  public void setUp() throws Exception {
    System.setProperty(SETTINGS_DIR_PROPERTY, TMP_DIR);
    propertiesFile = Files.createTempFile(TEST_FILE_PREFIX, "");
    devPropertiesFile = Files.createFile(Paths.get(propertiesFile.toString() + DEFAULT_DEV_FILE_EXT));
  }

  @AfterEach
  public void tearDown() throws Exception {
    System.clearProperty(SETTINGS_DIR_PROPERTY);
    Files.deleteIfExists(propertiesFile);
    Files.deleteIfExists(devPropertiesFile);
  }

  @Test
  public void fromFilesInSettingsDirShouldLoadProperties() throws Exception {
    String testKey = "testProperty";
    String testValue = "123";
    Files.write(propertiesFile, String.format("%s=%s", testKey, testValue).getBytes());

    Properties properties = fromFilesInSettingsDir(propertiesFile.getFileName().toString());

    assertEquals(1, properties.size());
    assertEquals(testValue, properties.getProperty(testKey));
  }

  @Test
  public void fromFilesInSettingsDirShouldOverrideDevProperties() throws Exception {
    String testKey = "testProperty";
    String testValue = "123";
    String testOverrideValue = "42";
    Files.write(propertiesFile, String.format("%s=%s", testKey, testValue).getBytes());
    Files.write(devPropertiesFile, String.format("%s=%s", testKey, testOverrideValue).getBytes());

    Properties properties = fromFilesInSettingsDir(propertiesFile.getFileName().toString());

    assertEquals(1, properties.size());
    assertEquals(testOverrideValue, properties.getProperty(testKey));
  }

  @Test
  public void testGetInteger() {
    assertEquals(Integer.MAX_VALUE, PropertiesUtils.getInteger(testProperties, "intProperty").intValue());
    assertEquals(Integer.MIN_VALUE, PropertiesUtils.getInteger(testProperties, "notExistedProperty", Integer.MIN_VALUE));
    assertNull(PropertiesUtils.getInteger(testProperties, "notExistedProperty"));
  }

  @Test
  public void testGetLong() {
    assertEquals(Long.MAX_VALUE, PropertiesUtils.getLong(testProperties, "longProperty").longValue());
    assertEquals(Long.MIN_VALUE, PropertiesUtils.getLong(testProperties, "notExistedProperty", Long.MIN_VALUE));
    assertNull(PropertiesUtils.getLong(testProperties, "notExistedProperty"));
  }

  @Test
  public void testGetDouble() {
    assertEquals(Double.MAX_VALUE, PropertiesUtils.getDouble(testProperties, "doubleProperty").doubleValue());
    assertEquals(Double.MIN_VALUE, PropertiesUtils.getDouble(testProperties, "notExistedProperty", Double.MIN_VALUE));
    assertNull(PropertiesUtils.getDouble(testProperties, "notExistedProperty"));
  }

  @Test
  public void testGetBoolean() {
    assertTrue(PropertiesUtils.getBoolean(testProperties, "boolProperty"));
    assertFalse(PropertiesUtils.getBoolean(testProperties, "notExistedProperty", false));
    assertNull(PropertiesUtils.getBoolean(testProperties, "notExistedProperty"));
  }

  @Test
  public void testGetStringList() {
    List<String> settings = PropertiesUtils.getStringList(testProperties, "listProperty");

    assertEquals(3, settings.size());
    assertEquals("value1", settings.get(0));
    assertEquals("value2", settings.get(1));
    assertEquals("value3", settings.get(2));
  }

  @Test
  public void testGetNotEmptyOrThrow() {
    assertEquals("value", PropertiesUtils.getNotEmptyOrThrow(testProperties, "stringProperty"));
  }

  @Test
  public void testGetNotEmptyOrThrowThrowsExceptionIfPropertyDoesNotExist() {
    assertThrows(IllegalStateException.class, () -> PropertiesUtils.getNotEmptyOrThrow(testProperties, "notExistedProperty"));
  }

  @Test
  public void testGetNotEmptyOrThrowThrowsExceptionIfPropertyIsEmpty() {
    assertThrows(IllegalStateException.class, () -> PropertiesUtils.getNotEmptyOrThrow(testProperties, "emptyProperty"));
  }

  @Test
  public void testGetAsMap() {
    Properties properties = new Properties();
    properties.put("key1", "value1");
    properties.put("key2", "value2");
    Map<String, String> propertiesMap = PropertiesUtils.getAsMap(properties);

    assertEquals(2, propertiesMap.size());
    assertTrue(propertiesMap.containsKey("key1"));
    assertTrue(propertiesMap.containsKey("key2"));
    assertEquals("value1", propertiesMap.get("key1"));
    assertEquals("value2", propertiesMap.get("key2"));
  }

  @Test
  public void testGetPropertiesStartWith() {
    Properties propertiesWithPrefix = PropertiesUtils.getPropertiesStartWith(testProperties, "namespace");

    assertEquals(2, propertiesWithPrefix.size());
    assertTrue(propertiesWithPrefix.containsKey("namespace.property1"));
    assertTrue(propertiesWithPrefix.containsKey("namespace.property2"));
    assertEquals("value1", propertiesWithPrefix.getProperty("namespace.property1"));
    assertEquals("value2", propertiesWithPrefix.getProperty("namespace.property2"));
  }

  @Test
  public void testGetPropertiesStartWithThrowsExceptionIfPrefixIsEmpty() {
    assertThrows(IllegalArgumentException.class, () -> PropertiesUtils.getPropertiesStartWith(testProperties, ""));
  }

  @Test
  public void testGetPropertiesStartWithThrowsExceptionIfPrefixIsNull() {
    assertThrows(IllegalArgumentException.class, () -> PropertiesUtils.getPropertiesStartWith(testProperties, null));
  }

  @Test
  public void testGetSubProperties() {
    Properties subProperties = PropertiesUtils.getSubProperties(testProperties, "namespace");

    assertEquals(2, subProperties.size());
    assertTrue(subProperties.containsKey("property1"));
    assertTrue(subProperties.containsKey("property2"));
    assertEquals("value1", subProperties.getProperty("property1"));
    assertEquals("value2", subProperties.getProperty("property2"));
  }

  @Test
  public void testGetSubPropertiesThrowsExceptionIfPrefixIsEmpty() {
    assertThrows(IllegalArgumentException.class, () -> PropertiesUtils.getSubProperties(testProperties, ""));
  }

  @Test
  public void testGetSubPropertiesThrowsExceptionIfPrefixIsNull() {
    assertThrows(IllegalArgumentException.class, () -> PropertiesUtils.getSubProperties(testProperties, null));
  }

  @Test
  public void testSetSystemPropertyIfAbsent() {
    String testValue = "tmp";

    System.clearProperty(SETTINGS_DIR_PROPERTY);

    PropertiesUtils.setSystemPropertyIfAbsent(SETTINGS_DIR_PROPERTY, testValue);

    assertEquals(testValue, System.getProperty(SETTINGS_DIR_PROPERTY));

    PropertiesUtils.setSystemPropertyIfAbsent(SETTINGS_DIR_PROPERTY, "some new value");

    assertEquals(testValue, System.getProperty(SETTINGS_DIR_PROPERTY));
  }
}
