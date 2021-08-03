package ru.hh.nab.common.properties;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import static java.util.stream.Collectors.toMap;
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

    Map.Entry<Properties, Map<String, Path>> propertiesAndOverrideLocations = loadFromFileWithOverrides(filePath);
    Properties config = propertiesAndOverrideLocations.getKey();
    Map<String, Path> overrideLocations = propertiesAndOverrideLocations.getValue();

    Path devConfigPath = filePath.getParent().resolve(devFileName);
    if (Files.isReadable(devConfigPath)) {
      Properties devConfig = loadPropertiesFromPath(devConfigPath);
      config.putAll(devConfig);
      overrideLocations.putAll(devConfig.stringPropertyNames().stream().collect(toMap(Function.identity(), v -> devConfigPath)));
    }

    overrideLocations.forEach((block, path) -> System.out.println(block + " is effectively loaded from " + path));

    return config;
  }

  public static Map.Entry<Properties, Map<String, Path>> loadFromFileWithOverrides(Path filePath) throws IOException {
    Properties initialConfig = loadPropertiesFromPath(filePath);
    Path overridesDirPath = filePath.getParent().resolve(filePath.getFileName() + OVERRIDE_FOLDER_POSTFIX);
    Map<String, Path> overrideAddress = new HashMap<>();
    if (Files.exists(overridesDirPath) && Files.isDirectory(overridesDirPath)) {
      try (Stream<Path> fileStream = Files.list(overridesDirPath).filter(Files::isRegularFile).sorted()) {
        fileStream.forEachOrdered(path -> {
          Properties overrideConfig = loadPropertiesFromPath(path);
          initialConfig.putAll(overrideConfig);
          overrideAddress.putAll(overrideConfig.stringPropertyNames().stream().collect(toMap(Function.identity(), v -> path)));
        });
      }
    }
    return Map.entry(initialConfig, overrideAddress);
  }

  public static Properties loadPropertiesFromPath(Path path) {
    Properties props = new Properties();
    try (InputStream inputStream = Files.newInputStream(path)) {
      props.load(inputStream);
      return props;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static void setSystemPropertyIfAbsent(final String name, final String value) {
    if (System.getProperty(name) == null) {
      System.setProperty(name, value);
    }
  }
}
