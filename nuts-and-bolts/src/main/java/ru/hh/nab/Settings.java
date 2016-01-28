package ru.hh.nab;

import java.util.Properties;

public class Settings {
  private final Properties props;
  public final int concurrencyLevel;
  public final int workersQueueLimit;
  public final int port;

  public Settings(Properties props) {
    this.props = props;
    concurrencyLevel = Integer.parseInt(props.getProperty("concurrencyLevel"));
    port = Integer.parseInt(props.getProperty("port"));
    workersQueueLimit = Integer.parseInt(props.getProperty("workersQueueLimit", "-1"));
  }

  public Properties subTree(String prefix) {
    return subTree(prefix, null);
  }

  public Properties subTree(String prefix, String newPrefix) {
    prefix = prefix + ".";
    if (newPrefix != null) {
      newPrefix = newPrefix + ".";
    } else {
      newPrefix = "";
    }
    Properties ret = new Properties();
    for (String i : props.stringPropertyNames()) {
      if (!i.startsWith(prefix)) {
        continue;
      }
      String suffix = i.substring(prefix.length());
      ret.put(newPrefix + suffix, props.get(i));
    }
    return ret;
  }

  public final static class BoolProperty {
    final boolean defaultValue;
    final String propertyName;
    public BoolProperty(final String propertyName, final boolean defaultValue) {
      this.propertyName = propertyName;
      this.defaultValue = defaultValue;
    }
    public boolean getFrom(final Properties properties) {
      String propertyValue = properties.getProperty(propertyName);
      return propertyValue == null ? defaultValue : Boolean.parseBoolean(propertyValue);
    }
  }

  public final static class IntProperty {
    final int defaultValue;
    final String propertyName;
    public IntProperty(final String propertyName, final int defaultValue) {
      this.propertyName = propertyName;
      this.defaultValue = defaultValue;
    }
    public int from(final Properties properties) {
      String propertyValue = properties.getProperty(propertyName);
      return propertyValue == null ? defaultValue : Integer.parseInt(propertyValue);
    }
  }

  public final static class StringProperty {
    final String defaultValue;
    final String propertyName;
    public StringProperty(final String propertyName, final String defaultValue) {
      this.propertyName = propertyName;
      this.defaultValue = defaultValue;
    }
    public String getFrom(final Properties properties) {
      return properties.getProperty(propertyName, defaultValue);
    }
  }
}
