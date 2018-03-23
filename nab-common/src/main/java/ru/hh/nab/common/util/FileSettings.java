package ru.hh.nab.common.util;

import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.List;
import java.util.Properties;
import static org.springframework.util.Assert.hasLength;

public class FileSettings {
  private final Properties properties;

  public FileSettings(final Properties properties) {
    this.properties = properties;
  }

  public String getString(String key) {
    return properties.getProperty(key);
  }

  public Integer getInteger(String key) {
    String value = getString(key);
    return value != null ? Integer.parseInt(value): null;
  }

  public Long getLong(final String key) {
    String value = getString(key);
    return value != null ? Long.parseLong(value): null;
  }

  public Boolean getBoolean(String key) {
    String value = getString(key);
    return value != null ? Boolean.parseBoolean(value): null;
  }

  public Properties getSubProperties(String prefix) {
    hasLength(prefix, "prefix should not be null or empty");
    final Properties subProperties = new Properties();
    properties.stringPropertyNames().stream()
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
    return value != null ? asList(value.split("[,\\s]+")) : new ArrayList<>();
  }

  public Properties getProperties() {
    Properties propertiesCopy = new Properties();
    propertiesCopy.putAll(this.properties);
    return propertiesCopy;
  }
}
