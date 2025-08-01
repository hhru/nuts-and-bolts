package ru.hh.nab.common.properties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import static java.util.function.Function.identity;
import java.util.stream.Collectors;

public final class PropertiesUtils {
  public static final String SETTINGS_DIR_PROPERTY = "settingsDir";
  static final String DEFAULT_DEV_FILE_EXT = ".dev";

  private PropertiesUtils() {
  }

  /**
   * @deprecated
   * Import all necessary files from settingsDir through spring.config.import parameter in application.properties file.
   * All properties will be added to spring Environment.
   */
  @Deprecated(forRemoval = true)
  public static Properties fromFilesInSettingsDir(String fileName) throws IOException {
    return fromFilesInSettingsDir(fileName, fileName + DEFAULT_DEV_FILE_EXT);
  }

  /**
   * @deprecated
   * Import all necessary files from settingsDir through spring.config.import parameter in application.properties file.
   * All properties will be added to spring Environment.
   */
  @Deprecated(forRemoval = true)
  public static Properties fromFilesInSettingsDir(String fileName, String devFileName) throws IOException {
    final String settingsDir = System.getProperty(SETTINGS_DIR_PROPERTY, ".");
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

  public static int getInteger(Properties properties, String key, int defaultValue) {
    return parseValueOrDefault(properties, key, Integer::parseInt, defaultValue);
  }

  public static Integer getInteger(Properties properties, String key) {
    return parseValueOrDefault(properties, key, Integer::parseInt, null);
  }

  public static long getLong(Properties properties, String key, long defaultValue) {
    return parseValueOrDefault(properties, key, Long::parseLong, defaultValue);
  }

  public static Long getLong(Properties properties, String key) {
    return parseValueOrDefault(properties, key, Long::parseLong, null);
  }

  public static double getDouble(Properties properties, String key, double defaultValue) {
    return parseValueOrDefault(properties, key, Double::parseDouble, defaultValue);
  }

  public static Double getDouble(Properties properties, String key) {
    return parseValueOrDefault(properties, key, Double::parseDouble, null);
  }

  public static boolean getBoolean(Properties properties, String key, boolean defaultValue) {
    return parseValueOrDefault(properties, key, Boolean::parseBoolean, defaultValue);
  }

  public static Boolean getBoolean(Properties properties, String key) {
    return parseValueOrDefault(properties, key, Boolean::parseBoolean, null);
  }

  public static List<String> getStringList(Properties properties, String key) {
    String value = properties.getProperty(key);
    if (value == null || value.isBlank()) {
      return new ArrayList<>();
    }
    return asList(value.split("[,\\s]+"));
  }

  public static String getNotEmptyOrThrow(Properties properties, String key) {
    String property = properties.getProperty(key);
    if (property == null || property.isEmpty()) {
      throw new IllegalStateException(key + " in configuration must not be empty");
    }
    return property;
  }

  public static Map<String, String> getAsMap(Properties properties) {
    return properties
        .stringPropertyNames()
        .stream()
        .collect(Collectors.toUnmodifiableMap(identity(), properties::getProperty));
  }

  public static Properties getPropertiesStartWith(Properties properties, String prefix) {
    if (prefix == null || prefix.isEmpty()) {
      throw new IllegalArgumentException("prefix should not be null or empty");
    }
    Properties propertiesWithPrefix = new Properties();
    properties
        .stringPropertyNames()
        .stream()
        .filter(propertyName -> propertyName.startsWith(prefix + "."))
        .forEach(propertyName -> propertiesWithPrefix.setProperty(propertyName, properties.getProperty(propertyName)));
    return propertiesWithPrefix;
  }

  public static Properties getSubProperties(Properties properties, String prefix) {
    if (prefix == null || prefix.isEmpty()) {
      throw new IllegalArgumentException("prefix should not be null or empty");
    }
    Properties subProperties = new Properties();
    properties
        .stringPropertyNames()
        .stream()
        .filter(key -> key.startsWith(prefix + "."))
        .forEach(key -> {
          String newKey = key.substring(prefix.length() + 1);
          subProperties.put(newKey, properties.getProperty(key));
        });
    return subProperties;
  }

  public static void setSystemPropertyIfAbsent(final String name, final String value) {
    if (System.getProperty(name) == null) {
      System.setProperty(name, value);
    }
  }

  private static <R> R parseValueOrDefault(Properties properties, String key, Function<String, R> function, R defaultValue) {
    String value = properties.getProperty(key);
    return value == null ? defaultValue : function.apply(value);
  }
}
