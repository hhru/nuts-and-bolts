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

  public static int getIntProperty(final Properties properties, final String propertyName, final int defaultValue) {
    String propertyValue = properties.getProperty(propertyName);
    return propertyValue == null ? defaultValue : Integer.parseInt(propertyValue);
  }

  public static boolean getBoolProperty(final Properties properties, final String propertyName, final boolean defaultValue) {
    String propertyValue = properties.getProperty(propertyName);
    return propertyValue == null ? defaultValue : Boolean.parseBoolean(propertyValue);
  }
}
