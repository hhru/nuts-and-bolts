package ru.hh.nab.starter;

import static java.text.MessageFormat.format;

import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.context.ApplicationContext;
import ru.hh.nab.common.properties.FileSettings;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;

public final class NabApplication {
  private static final Logger LOGGER = LoggerFactory.getLogger(NabApplication.class);

  public static NabApplicationContext run(Class<?>... configurationClasses) {
    return run(new NabServletContextConfig(), configurationClasses);
  }

  public static NabApplicationContext run(NabServletContextConfig servletContextConfig, Class<?>... configurationClasses) {
    configureLogger();
    NabApplicationContext context = null;
    try {
      context = new NabApplicationContext(servletContextConfig, configurationClasses);
      context.startApplication();
      configureSentry(context);
      logStartupInfo(context);
    } catch (Exception e) {
      logErrorAndExit(e);
    }
    return context;
  }

  public static void configureLogger() {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  public static void configureSentry(ApplicationContext context) {
    FileSettings settings = context.getBean(FileSettings.class);
    Sentry.init(settings.getString("sentry.dsn"));
  }

  private static void logStartupInfo(ApplicationContext context) {
    AppMetadata appMetadata = context.getBean(AppMetadata.class);
    LOGGER.info("Started {} PID={} (version={})", appMetadata.getServiceName(), getCurrentPid(), appMetadata.getVersion());
  }

  private static String getCurrentPid() {
    return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
  }

  private static void logErrorAndExit(Exception e) {
    try {
      LOGGER.error("Failed to start, shutting down", e);
      System.err.println(format("[{0}] Failed to start, shutting down: {1}", LocalDateTime.now(), e.getMessage()));
    } finally {
      System.exit(1);
    }
  }
}
