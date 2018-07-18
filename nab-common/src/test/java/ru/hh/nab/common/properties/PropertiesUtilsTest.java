package ru.hh.nab.common.properties;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import static ru.hh.nab.common.properties.PropertiesUtils.SETINGS_DIR_PROPERTY;
import static ru.hh.nab.common.properties.PropertiesUtils.fromFilesInSettingsDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class PropertiesUtilsTest {
  private static final String TMP_DIR = System.getProperty("java.io.tmpdir");
  private static final String TEST_FILE_PREFIX = "settings.properties";
  private Path propertiesFile;
  private Path devPropertiesFile;

  @Before
  public void setUp() throws Exception {
    System.setProperty(SETINGS_DIR_PROPERTY, TMP_DIR);
    propertiesFile = Files.createTempFile(TEST_FILE_PREFIX, "");
    devPropertiesFile = Files.createTempFile(TEST_FILE_PREFIX, "");
  }

  @After
  public void tearDown() throws Exception {
    System.clearProperty(SETINGS_DIR_PROPERTY);
    Files.deleteIfExists(propertiesFile);
    Files.deleteIfExists(devPropertiesFile);
  }

  @Test
  public void fromFilesInSettingsDirShouldLoadProperties() throws Exception {
    String testKey = "testProperty";
    String testValue = "123";
    Files.write(propertiesFile, String.format("%s=%s", testKey, testValue).getBytes());

    Properties properties = fromFilesInSettingsDir(
        propertiesFile.getFileName().toString(), devPropertiesFile.getFileName().toString());

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

    Properties properties = fromFilesInSettingsDir(
        propertiesFile.getFileName().toString(), devPropertiesFile.getFileName().toString());

    assertEquals(1, properties.size());
    assertEquals(testOverrideValue, properties.getProperty(testKey));
  }

  @Test
  public void testSetSystemPropertyIfAbsent() {
    String testValue = "tmp";

    System.clearProperty(SETINGS_DIR_PROPERTY);

    PropertiesUtils.setSystemPropertyIfAbsent(SETINGS_DIR_PROPERTY, testValue);

    assertEquals(testValue, System.getProperty(SETINGS_DIR_PROPERTY));

    PropertiesUtils.setSystemPropertyIfAbsent(SETINGS_DIR_PROPERTY, "some new value");

    assertEquals(testValue, System.getProperty(SETINGS_DIR_PROPERTY));
  }
}
