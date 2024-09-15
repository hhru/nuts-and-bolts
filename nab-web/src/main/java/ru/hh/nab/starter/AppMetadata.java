package ru.hh.nab.starter;

import java.util.concurrent.TimeUnit;

public class AppMetadata {

  private final String serviceName;
  private final String version;
  private final long started;

  public AppMetadata(String serviceName, String version) {
    this.serviceName = serviceName;
    this.version = version;
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
