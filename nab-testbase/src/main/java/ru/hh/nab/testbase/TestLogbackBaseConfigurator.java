package ru.hh.nab.testbase;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.status.NopStatusListener;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import static ru.hh.nab.common.properties.PropertiesUtils.SETINGS_DIR_PROPERTY;
import ru.hh.nab.logging.HhMultiAppender;
import ru.hh.nab.logging.NabLoggingConfiguratorTemplate;
import static ru.hh.nab.testbase.NabTestConfig.TEST_PROPERTIES_FILE_NAME;

public class TestLogbackBaseConfigurator extends NabLoggingConfiguratorTemplate {

  @Override
  protected Properties createLoggingProperties() {
    System.setProperty(SETINGS_DIR_PROPERTY, ".");
    Properties properties = createProperties();
    setPropertyIfNotSet(properties, "log.pattern", "[%date{ISO8601}] %-5level %logger{36}:%line mdc={%mdc} - %msg%n");
    setPropertyIfNotSet(properties, "log.dir", "target/logs");
    setPropertyIfNotSet(properties, "log.immediate.flush", Boolean.TRUE.toString());
    return properties;
  }

  @Override
  protected void statusListener(LoggerContext context, Properties properties) {
    if (Boolean.parseBoolean(properties.getProperty("log.show.status", "false"))) {
      super.statusListener(context, properties);
    } else {
      context.getStatusManager().add(new NopStatusListener());
    }
  }

  @Override
  public final void configure(LoggingContextWrapper context) {
    var rootLogger = getRootLogger(context);
    Level rootLevel = context.getProperty("log.root.level", Level.ERROR, level -> Level.valueOf(level.toUpperCase()));
    rootLogger.setLevel(rootLevel);
    Set<Appender> appenders = new HashSet<>();
    if (Boolean.parseBoolean(context.getProperty("log.toConsole", "true"))) {
      appenders.add(createAppender(context, "console", () -> new HhMultiAppender(false)));
    }
    loggers().forEach((name, level) -> createLogger(context, name, level.toString(), false, appenders));
    rootLogger.addAppenders(appenders);
  }

  protected Map<String, Level> loggers() {
    return new HashMap<>() {
      {
        put("org.hibernate.tool.hbm2ddl", Level.INFO);
        put("org.hibernate.orm.deprecation", Level.OFF);
        put("com.opentable.db.postgres.embedded", Level.INFO);
        put("org.testcontainers", Level.INFO);
        put("org.testcontainers.utility.ResourceReaper", Level.OFF);
      }
    };
  }

  protected Properties createProperties() {
    try {
      Properties properties = new Properties();
      properties.load(this.getClass().getResourceAsStream("/" + TEST_PROPERTIES_FILE_NAME));
      return properties;
    } catch (NullPointerException e) {
      return new Properties();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
