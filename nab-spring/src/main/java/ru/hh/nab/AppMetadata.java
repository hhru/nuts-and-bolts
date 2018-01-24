package ru.hh.nab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Properties;

public class AppMetadata {

  private static final Logger LOGGER = LoggerFactory.getLogger(AppMetadata.class);

  private static final String PROJECT_PROPERTIES = "/project.properties";

  private final String name;
  private final String version;
  private final long started;

  AppMetadata(String name) {
    Properties projectProps = new Properties();

    try (InputStream s = AppMetadata.class.getResourceAsStream(PROJECT_PROPERTIES)) {
      projectProps.load(s);
    }
    catch (Exception e) {
      LOGGER.warn("Failed to load {}, project version will be unknown, ignoring", PROJECT_PROPERTIES, e);
    }

    this.name = name;
    version = projectProps.getProperty("project.version", "unknown");
    started = System.currentTimeMillis();
  }

  public String getStatus() {
    LocalDateTime start = Instant.ofEpochMilli(started).atZone(ZoneId.systemDefault()).toLocalDateTime();
    return MessageFormat.format("[{0}] {1} (ver. {2}) started at {3}", LocalDateTime.now(), name, version, start);
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  public long getUpTimeSeconds() {
    return (System.currentTimeMillis() - started) / 1000;
  }
}
