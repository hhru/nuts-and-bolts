package ru.hh.nab.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class AppMetadata {

  private static final Logger LOGGER = LoggerFactory.getLogger(AppMetadata.class);

  private static final String PROJECT_PROPERTIES = "/project.properties";

  private final String serviceName;
  private final String version;
  private final long started;

  AppMetadata(String serviceName) {
    Properties projectProps = new Properties();

    try (InputStream s = AppMetadata.class.getResourceAsStream(PROJECT_PROPERTIES)) {
      projectProps.load(s);
    }
    catch (Exception e) {
      LOGGER.warn("Failed to load {}, project version will be unknown, ignoring", PROJECT_PROPERTIES, e);
    }

    this.serviceName = serviceName;

    version = projectProps.getProperty("project.version", "unknown");
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
