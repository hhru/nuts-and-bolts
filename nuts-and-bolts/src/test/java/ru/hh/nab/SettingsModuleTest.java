package ru.hh.nab;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Properties;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SettingsModuleTest {

  @Test
  public void testLoadProperties() throws IOException {
    Properties defaultSettings = new Properties() {{
      put("port", "1111");
    }};
    File settingsDir = prepareSettingsDir(defaultSettings, null);
    Properties props = SettingsModule.loadProperties(settingsDir);
    assertEquals("1111", props.getProperty("port"));
  }

  @Test
  public void testLoadPropertiesOverriden() throws IOException {
    Properties defaultSettings = new Properties() {{
      put("port", "1111");
    }};
    Properties overrideSettings = new Properties() {{
      put("port", "2222");
    }};
    File settingsDir = prepareSettingsDir(defaultSettings, overrideSettings);
    Properties props = SettingsModule.loadProperties(settingsDir);
    assertEquals("2222", props.getProperty("port"));
  }

  private static File prepareSettingsDir(Properties defaultSettings, Properties overrideSettings) throws IOException {
    File directory = Files.createTempDirectory(SettingsModuleTest.class.getName()).toFile();
    directory.mkdirs();
    writeProperties(directory, defaultSettings, "settings.properties");
    writeProperties(directory, overrideSettings, "settings.properties.dev");
    return directory;
  }

  private static void writeProperties(File directory, Properties settings, String fileName) throws IOException {
    if (settings != null) {
      File targetFile = new File(directory, fileName);
      targetFile.createNewFile();
      try (Writer writer = new FileWriter(targetFile)) {
        settings.store(writer, "");
      }
    }
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
