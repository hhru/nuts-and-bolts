package ru.hh.nab;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.Properties;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SettingsModuleTest {

  @Test
  public void testLoadProperties() throws IOException {
    File settingsDir = prepareSettingsDir("port=1111", null);
    Properties props = SettingsModule.loadProperties(settingsDir);
    assertEquals("1111", props.getProperty("port"));
  }

  @Test
  public void testLoadPropertiesOverriden() throws IOException {
    File settingsDir = prepareSettingsDir("port=1111", "port=2222");
    Properties props = SettingsModule.loadProperties(settingsDir);
    assertEquals("2222", props.getProperty("port"));
  }

  private static File prepareSettingsDir(String defaultValue, String overrideValue) throws IOException {
    File directory = Files.createTempDirectory(SettingsModuleTest.class.getName()).toFile();
    directory.mkdirs();
    if (defaultValue != null) {
      File defaultProperties = new File(directory, "settings.properties");
      defaultProperties.createNewFile();
      Files.write(defaultProperties.toPath(), Collections.singleton(defaultValue));
    }
    if (overrideValue != null) {
      File properties = new File(directory, "settings.properties.dev");
      properties.createNewFile();
      Files.write(properties.toPath(), Collections.singleton(overrideValue));
    }
    return directory;
  }

  @After
  public void removeSettings() throws IOException {
    File directory = Files.createTempDirectory(SettingsModuleTest.class.getName()).toFile();
    Files.walk(directory.toPath(), FileVisitOption.FOLLOW_LINKS)
      .sorted(Comparator.reverseOrder())
      .map(Path::toFile)
      .forEach(File::delete);
  }
}
