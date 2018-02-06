package ru.hh.nab.core.util;

import com.google.common.base.Strings;

import java.util.Properties;

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
    return getSubProperties(prefix, null);
  }

  public Properties getSubProperties(String prefix, String newPrefix) {
    final String namespacePrefix = Strings.isNullOrEmpty(prefix) ? "" : prefix + ".";
    final Properties subProperties = new Properties();

    properties.stringPropertyNames().stream()
        .filter(key -> key.startsWith(namespacePrefix))
        .forEach(key -> {
          String newKey = Strings.isNullOrEmpty(newPrefix) ? "" : newPrefix + ".";
          newKey += prefix.isEmpty() ? key : key.substring(prefix.length() + 1);
          subProperties.put(newKey, properties.getProperty(key));
        });

    return subProperties;
  }

  public FileSettings getSubSettings(String prefix) {
    return new FileSettings(getSubProperties(prefix));
  }
}
