package ru.hh.nab;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SettingsModuleTest {

  @Test
  public void testLoadProperties() throws IOException {
    File settingsDir = prepareSettingsDir(SettingsModuleTest.class.getName(), "port=1111", null);
    Properties props = SettingsModule.loadProperties(settingsDir);
    assertEquals("1111", props.getProperty("port"));
  }

  @Test
  public void testLoadPropertiesOverriden() throws IOException {
    File settingsDir = prepareSettingsDir(SettingsModuleTest.class.getName(), "port=1111", "port=2222");
    Properties props = SettingsModule.loadProperties(settingsDir);
    assertEquals("2222", props.getProperty("port"));
  }

  private File prepareSettingsDir(String dirName, String defaultValue, String actualValue) throws IOException {
    File directory = new File(System.getProperty("java.io.tmpdir"), dirName);
    directory.mkdirs();
    if (defaultValue != null) {
      File defaultProperties = new File(directory, "settings.properties");
      defaultProperties.createNewFile();
      FileUtils.write(defaultProperties, defaultValue, "UTF-8");
    }
    if (actualValue != null) {
      File properties = new File(directory, "settings.properties.dev");
      properties.createNewFile();
      FileUtils.write(properties, actualValue, "UTF-8");
    }
    return directory;
  }

  @After
  public void removeSettings() throws IOException {
    File testDir = new File(System.getProperty("java.io.tmpdir"), SettingsModuleTest.class.getName());
    FileUtils.deleteDirectory(testDir);
  }
}
