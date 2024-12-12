package ru.hh.nab.common.properties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.util.Optional.ofNullable;
import java.util.Properties;
import java.util.function.Predicate;
import static ru.hh.nab.common.qualifier.NamedQualifier.NODE_NAME;

public class PropertiesUtils {
  private static final String NODE_NAME_ENV = "NODE_NAME";
  public static final String SETINGS_DIR_PROPERTY = "settingsDir";
  static final String DEFAULT_DEV_FILE_EXT = ".dev";

  public static String getNodeName(FileSettings fileSettings) {
    return ofNullable(System.getenv(NODE_NAME_ENV))
        .orElseGet(
            () -> ofNullable(fileSettings.getString(NODE_NAME))
                .filter(Predicate.not(String::isEmpty))
                .orElseThrow(() -> new RuntimeException(String.format("'%s' property is not found in file settings", NODE_NAME)))
        );
  }

  public static Properties fromFilesInSettingsDir(String fileName) throws IOException {
    return fromFilesInSettingsDir(fileName, fileName + DEFAULT_DEV_FILE_EXT);
  }

  public static Properties fromFilesInSettingsDir(String fileName, String devFileName) throws IOException {
    final String settingsDir = System.getProperty(SETINGS_DIR_PROPERTY, ".");
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
