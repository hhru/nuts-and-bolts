package ru.hh.nab;

import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.util.Properties;

@Singleton
public class AppMetadata {

  private static final Logger LOGGER = LoggerFactory.getLogger(AppMetadata.class);

  private static final String PROJECT_PROPERTIES = "/project.properties";

  private final String name;
  private final String version;
  private final long started;

  public AppMetadata(String name) {
    Properties projectProps = new Properties();

    try (InputStream s = AppMetadata.class.getResourceAsStream(PROJECT_PROPERTIES)) {
      projectProps.load(s);
    }
    catch (Exception e) {
      LOGGER.warn("Failed to load {}, project version will be unknown, ignoring", PROJECT_PROPERTIES, e);
    }

    this.name = name;
    version = projectProps.getProperty("project.version", "unknown");
    this.started = System.currentTimeMillis();
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  public long getStarted() {
    return started;
  }
}
