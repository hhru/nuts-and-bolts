package ru.hh.nab.starter;

import ch.qos.logback.classic.Level;
import java.util.Properties;
import static ru.hh.nab.common.properties.PropertiesUtils.SETINGS_DIR_PROPERTY;
import ru.hh.nab.logging.NabLoggingConfiguratorTemplate;

public class NabStarterTestLogbackBaseConfigurator extends NabLoggingConfiguratorTemplate {

  @Override
  protected Properties createLoggingProperties() {
    System.setProperty(SETINGS_DIR_PROPERTY, ".");
    Properties properties = new Properties();
    setPropertyIfNotSet(properties, "log.pattern", "[%date{ISO8601}] %-5level %logger{36}:%line mdc={%mdc} - %msg%n");
    setPropertyIfNotSet(properties, "log.dir", "logs");
    setPropertyIfNotSet(properties, "log.immediate.flush", Boolean.TRUE.toString());
    return properties;
  }

  @Override
  public void configure(LoggingContextWrapper context) {
    var rootLogger = getRootLogger(context);
    rootLogger.setLevel(Level.ERROR);
  }
}
