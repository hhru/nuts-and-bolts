package ru.hh.nab.common.properties;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class PropertiesUtils {
  public static final String SETINGS_DIR_PROPERTY = "settingsDir";
  static final String OVERRIDE_FOLDER_POSTFIX = ".d";
  static final String DEFAULT_DEV_FILE_EXT = ".dev";

  public static Properties fromFilesInSettingsDir(String fileName) throws IOException {
    return fromFilesInSettingsDir(fileName, fileName + DEFAULT_DEV_FILE_EXT);
  }

  public static Properties fromFilesInSettingsDir(String fileName, String devFileName) throws IOException {
    Path filePath = Paths.get(System.getProperty(SETINGS_DIR_PROPERTY, ".")).resolve(fileName);
    Properties properties = new Properties();

    loadFromFileWithOverrides(filePath, configPath -> {
      try (InputStream inputStream = Files.newInputStream(configPath)) {
        properties.load(inputStream);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });

    Path customPath = filePath.getParent().resolve(devFileName);
    if (Files.isReadable(customPath)) {
      try (InputStream inputStream = Files.newInputStream(customPath)) {
        properties.load(inputStream);
      }
    }

    return properties;
  }

  public static void loadFromFileWithOverrides(Path filePath, Consumer<Path> merger) throws IOException {
    try (InputStream inputStream = Files.newInputStream(filePath)) {
      merger.accept(filePath);
    }
    Path overridesDirPath = filePath.getParent().resolve(filePath.getFileName() + OVERRIDE_FOLDER_POSTFIX);
    if (Files.exists(overridesDirPath) && Files.isDirectory(overridesDirPath)) {
      try (Stream<Path> fileStream = Files.list(overridesDirPath).filter(Files::isRegularFile).sorted()) {
        fileStream.forEachOrdered(merger);
      }
    }
  }

  public static void setSystemPropertyIfAbsent(final String name, final String value) {
    if (System.getProperty(name) == null) {
      System.setProperty(name, value);
    }
  }
}
