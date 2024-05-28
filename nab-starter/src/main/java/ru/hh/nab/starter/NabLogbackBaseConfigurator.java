package ru.hh.nab.starter;

import io.sentry.logback.SentryAppender;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import java.util.stream.Stream;
import org.slf4j.event.Level;
import static ru.hh.nab.common.properties.PropertiesUtils.fromFilesInSettingsDir;
import ru.hh.nab.logging.HhMultiAppender;
import ru.hh.nab.logging.NabLoggingConfiguratorTemplate;
import ru.hh.nab.logging.json.NabTSOnlyJsonEncoder;
import ru.hh.nab.logging.json.NabTSOnlyJsonLayout;
import static ru.hh.nab.starter.NabProdConfig.PROPERTIES_FILE_NAME;
import ru.hh.nab.starter.consul.ConsulService;
import ru.hh.nab.starter.server.jetty.JettyServer;
import ru.hh.nab.starter.server.logging.StructuredRequestLogger;

public abstract class NabLogbackBaseConfigurator extends NabLoggingConfiguratorTemplate {

  private static final Map<String, ch.qos.logback.classic.Level> SUPPORTED_SENTRY_LEVELS = Stream
      .of(
          ch.qos.logback.classic.Level.ERROR,
          ch.qos.logback.classic.Level.WARN,
          ch.qos.logback.classic.Level.INFO,
          ch.qos.logback.classic.Level.DEBUG,
          ch.qos.logback.classic.Level.TRACE
      )
      .collect(toMap(level -> level.levelStr, identity()));

  @Override
  protected Properties createLoggingProperties() {
    if (isTestProfile()) {
      return new Properties();
    }

    Properties properties = createProperties();
    setPropertyIfNotSet(properties, "log.pattern", "[%date{ISO8601}] %-5level %logger{36}:%line mdc={%mdc} - %msg%n");
    setPropertyIfNotSet(properties, "log.dir", "logs");
    setPropertyIfNotSet(properties, "log.immediate.flush", Boolean.TRUE.toString());
    setPropertyIfNotSet(properties, "log.toConsole", Boolean.FALSE.toString());
    setPropertyIfNotSet(properties, "log.timings", Boolean.FALSE.toString());
    return properties;
  }

  protected Properties createProperties() {
    try {
      return fromFilesInSettingsDir(PROPERTIES_FILE_NAME);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public final void configure(LoggingContextWrapper context) {
    if (isTestProfile()) {
      return;
    }

    SentryAppender sentry = createAppender(context, "sentry", () -> {
      var sentryAppender = new SentryAppender();

      var eventLevel = getSentryLevel("sentry.level", context);
      sentryAppender.setMinimumEventLevel(eventLevel);

      var breadcrumbLevel = getSentryLevel("sentry.breadcrumb.level", context);
      sentryAppender.setMinimumBreadcrumbLevel(breadcrumbLevel);

      return sentryAppender;
    });

    HhMultiAppender service = createAppender(context, "service", () -> new HhMultiAppender(true));

    HhMultiAppender requests = createAppender(context, "requests", () -> {
      var multiAppender = new HhMultiAppender(true);
      multiAppender.setLayoutSupplier(NabTSOnlyJsonLayout::new);
      multiAppender.setEncoderSupplier(NabTSOnlyJsonEncoder::new);
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
    createLogger(context, "org.springframework.kafka", Level.WARN, false, List.of(libraries, sentry));
    createLogger(context, "com.datastax", Level.INFO, false, List.of(libraries, sentry));
    createLogger(context, NabApplication.class, Level.INFO, false, service, sentry);
    createLogger(context, ConsulService.class, Level.INFO, false, service, sentry);
    createLogger(context, JettyServer.class, Level.INFO, false, service, sentry);
    createLogger(context, "org.eclipse.jetty.server", Level.INFO, false, List.of(service, sentry));

    var jClientTransactionalCheck = createAppender(context, "jclient-tx", () -> new HhMultiAppender(true));
    createLogger(context, "ru.hh.nab.jclient.checks.TransactionalCheck", Level.WARN, false, List.of(jClientTransactionalCheck));

    var jclientGlobalTimeoutCheck = createAppender(context, "jclient-timeout-check", () -> new HhMultiAppender(true));
    createLogger(context, "ru.hh.jclient.common.check.GlobalTimeoutCheck", Level.WARN, false, List.of(jclientGlobalTimeoutCheck));

    HhMultiAppender slowRequests = createAppender(context, "slowRequests", () -> {
      var multiAppender = new HhMultiAppender(true);
      multiAppender.setLayoutSupplier(NabTSOnlyJsonLayout::new);
      multiAppender.setEncoderSupplier(NabTSOnlyJsonEncoder::new);
      return multiAppender;
    });
    createLogger(context, "slowRequests", Level.WARN, false, List.of(slowRequests));

    var rootLogger = getRootLogger(context);
    rootLogger.setLevel(context.getProperty("log.root.level", Level.WARN, level -> Level.valueOf(level.toUpperCase())));
    rootLogger.addAppenders(service, sentry);
    configure(context, service, libraries, sentry);
  }

  private static ch.qos.logback.classic.Level getSentryLevel(String levelKey, LoggingContextWrapper context) {
    var levelString = context.getProperty(levelKey, ch.qos.logback.classic.Level.ERROR.levelStr);
    var level = SUPPORTED_SENTRY_LEVELS.get(levelString.toUpperCase());
    if (level == null) {
      throw new IllegalArgumentException("Got unsupported level '%s' from property '%s'; try from %s (case doesn't matter)".formatted(
          levelString,
          levelKey,
          SUPPORTED_SENTRY_LEVELS.keySet()
      ));
    }

    return level;
  }

  private boolean isTestProfile() {
    return NabLogbackBaseConfigurator.class.getClassLoader().resources(NabCommonConfig.TEST_PROPERTIES_FILE_NAME).findAny().isPresent();
  }

  public abstract void configure(LoggingContextWrapper context, HhMultiAppender service, HhMultiAppender libraries, SentryAppender sentryAppender);
}
