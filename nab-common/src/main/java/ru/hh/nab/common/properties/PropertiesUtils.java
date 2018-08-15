package ru.hh.nab.common.properties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class PropertiesUtils {
  static final String SETINGS_DIR_PROPERTY = "settingsDir";
  static final String DEFAULT_DEV_FILE_EXT = ".dev";

  public static Properties fromFilesInSettingsDir(String fileName) throws IOException {
    return fromFilesInSettingsDir(fileName, fileName + DEFAULT_DEV_FILE_EXT);
  }

  public static Properties fromFilesInSettingsDir(String fileName, String devFileName) throws IOException {
    final String settingsDir = System.getProperty(SETINGS_DIR_PROPERTY);
    final Properties properties = new Properties();

    final Path defaultPath = Paths.get(settingsDir, fileName);
    try (InputStream inputStream = Files.newInputStream(defaultPath)) {
      properties.load(inputStream);
    }

    final Path customPath = Paths.get(settingsDir, devFileName);
    if (Files.isReadable(customPath)) {
      try (InputStream inputStream = Files.newInputStream(customPath)) {
        properties.load(inputStream);
      }
    }

    return properties;
  }

  public static void setSystemPropertyIfAbsent(final String name, final String value) {
    if (System.getProperty(name) == null) {
      System.setProperty(name, value);
    }
  }
}
