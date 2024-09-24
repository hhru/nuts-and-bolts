package ru.hh.nab.common.properties;

import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import static java.util.function.Function.identity;
import java.util.stream.Collectors;
import org.springframework.core.env.Environment;
import static org.springframework.util.Assert.hasLength;

/**
 * @deprecated Use spring mechanisms to get settings:
 * <ul>
 *   <li>{@link Environment}</li>
 *   <li>{@code @ConfigurationProperties}</li>
 *   <li>{@code @Value}</li>
 * </ul>
 */
@Deprecated(forRemoval = true)
public class FileSettings {
  private final Properties properties;

  public FileSettings(final Properties properties) {
    this.properties = properties;
  }

  public String getString(String key) {
    return properties.getProperty(key);
  }

  public String getString(String key, String defaultValue) {
    return Objects.requireNonNullElse(properties.getProperty(key), defaultValue);
  }

  public int getInteger(String key, int defaultValue) {
    return parseValueOrDefault(key, Integer::parseInt, defaultValue);
  }

  public Integer getInteger(String key) {
    return parseValueOrDefault(key, Integer::parseInt, null);
  }

  public long getLong(final String key, long defaultValue) {
    return parseValueOrDefault(key, Long::parseLong, defaultValue);
  }

  public Long getLong(final String key) {
    return parseValueOrDefault(key, Long::parseLong, null);
  }

  public double getDouble(final String key, double defaultValue) {
    return parseValueOrDefault(key, Double::parseDouble, defaultValue);
  }

  public Double getDouble(final String key) {
    return parseValueOrDefault(key, Double::parseDouble, null);
  }

  public boolean getBoolean(String key, boolean defaultValue) {
    return parseValueOrDefault(key, Boolean::parseBoolean, defaultValue);
  }

  public Boolean getBoolean(String key) {
    return parseValueOrDefault(key, Boolean::parseBoolean, null);
  }

  public Properties getSubProperties(String prefix) {
    hasLength(prefix, "prefix should not be null or empty");
    final Properties subProperties = new Properties();
    properties
        .stringPropertyNames()
        .stream()
        .filter(key -> key.startsWith(prefix + "."))
        .forEach(key -> {
          String newKey = prefix.isEmpty() ? key : key.substring(prefix.length() + 1);
          subProperties.put(newKey, properties.getProperty(key));
        });
    return subProperties;
  }

  public FileSettings getSubSettings(String prefix) {
    return new FileSettings(getSubProperties(prefix));
  }

  public List<String> getStringList(String key) {
    String value = getString(key);
    if (value == null || value.isBlank()) {
      return new ArrayList<>();
    }
    return asList(value.split("[,\\s]+"));
  }

  public Properties getProperties() {
    Properties propertiesCopy = new Properties();
    propertiesCopy.putAll(this.properties);
    return propertiesCopy;
  }

  public Map<String, ? extends String> getAsMap() {
    return properties
        .stringPropertyNames()
        .stream()
        .collect(Collectors.toUnmodifiableMap(identity(), properties::getProperty));
  }

  private <R> R parseValueOrDefault(String key, Function<String, R> function, R defaultValue) {
    String value = getString(key);
    return value == null ? defaultValue : function.apply(value);
  }

  public String getNotEmptyOrThrow(String propertyKey) {
    final String property = getString(propertyKey);
    if (property == null || property.isEmpty()) {
      throw new IllegalStateException(propertyKey + " in configuration must not be empty");
    }
    return property;
  }
}
