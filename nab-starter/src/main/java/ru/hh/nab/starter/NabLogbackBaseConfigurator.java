package ru.hh.nab.starter;

import ch.qos.logback.classic.filter.ThresholdFilter;
import io.sentry.logback.SentryAppender;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.slf4j.event.Level;
import ru.hh.nab.logging.HhMultiAppender;
import ru.hh.nab.logging.NabLoggingConfiguratorTemplate;
import ru.hh.nab.starter.server.logging.StructuredRequestJsonLayout;
import ru.hh.nab.starter.server.logging.StructuredRequestLogger;

public abstract class NabLogbackBaseConfigurator extends NabLoggingConfiguratorTemplate {

  @Override
  protected Properties createLoggingProperties() {
    Properties properties = createProperties();
    setPropertyIfNotSet(properties, "log.pattern", "[%date{ISO8601}] %-5level %logger{36}:%line mdc={%mdc} - %msg%n");
    setPropertyIfNotSet(properties, "log.dir", "logs");
    setPropertyIfNotSet(properties, "log.immediate.flush", Boolean.TRUE.toString());
    setPropertyIfNotSet(properties, "log.toConsole", Boolean.FALSE.toString());
    setPropertyIfNotSet(properties, "log.timings", Boolean.FALSE.toString());
    return properties;
  }

  protected Properties createProperties() {
    var settingsDir = System.getProperty("settingsDir");
    return loadPropertiesFile(Path.of(settingsDir).resolve("service.properties"));
  }

  @Override
  public final void configure(LoggingContextWrapper context) {
    SentryAppender sentry = createAppender(context, "sentry", () -> {
      var sentryAppender = new SentryAppender();
      var filter = new ThresholdFilter();
      filter.setLevel(context.getProperty("sentry.level", Level.ERROR.toString()));
      sentryAppender.addFilter(filter);
      return sentryAppender;
    });

    HhMultiAppender service = createAppender(context, "service", () -> new HhMultiAppender(true));

    HhMultiAppender requests = createAppender(context, "requests", () -> {
      var multiAppender = new HhMultiAppender(true);
      multiAppender.setLayout(new StructuredRequestJsonLayout());
      return multiAppender;
    });

    HhMultiAppender libraries = createAppender(context, "libraries", () -> new HhMultiAppender(true));

    createLogger(context, StructuredRequestLogger.class, Level.INFO, requests);
    createLogger(context, "org.hibernate", Level.WARN, false, List.of(libraries, sentry));
    createLogger(context, "com.mchange", Level.WARN, false, List.of(libraries, sentry));
    createLogger(context, "com.zaxxer.hikari", Level.WARN, false, List.of(libraries, sentry));
    createLogger(context, "io.sentry", Level.WARN, false, List.of(libraries, sentry));
    createLogger(context, "com.rabbitmq", Level.WARN, false, List.of(libraries, sentry));
    createLogger(context, "org.springframework.amqp", Level.WARN, false, List.of(libraries, sentry));
    createLogger(context, "net.spy.memcached", Level.WARN, false, List.of(libraries, sentry));
    createLogger(context, "org.glassfish.jersey", Level.WARN, false, List.of(libraries, sentry));
    createLogger(context, "com.datastax.driver", Level.INFO, false, List.of(libraries, sentry));

    var rootLogger = getRootLogger(context);
    rootLogger.setLevel(Level.valueOf(context.getProperty("log.root.level", Level.WARN.toString())));
    rootLogger.addAppenders(service, sentry);
    configure(context, service, libraries, sentry);
  }

  public abstract void configure(LoggingContextWrapper context, HhMultiAppender service, HhMultiAppender libraries, SentryAppender sentryAppender);
}
