package ru.hh.nab.starter;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class AppMetadata {
  public static final String PROJECT_PROPERTIES = "/project.properties";

  private final String serviceName;
  private final String version;
  private final long started;

  public AppMetadata(String serviceName, Properties projectProperties) {
    this.serviceName = serviceName;
    version = projectProperties.getProperty("project.version", "unknown");
    started = System.currentTimeMillis();
  }

  public String getServiceName() {
    return serviceName;
  }

  public String getVersion() {
    return version;
  }

  public long getUpTimeSeconds() {
    return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - started);
  }
}
