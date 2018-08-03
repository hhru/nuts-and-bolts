package ru.hh.nab.starter;

import static java.text.MessageFormat.format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.context.ApplicationContext;
import ru.hh.nab.starter.servlet.DefaultServletConfig;
import ru.hh.nab.starter.servlet.ServletConfig;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;

public final class NabApplication {
  private static final Logger LOGGER = LoggerFactory.getLogger(NabApplication.class);

  public static NabApplicationContext run(Class<?>... configurationClasses) {
    return run(new DefaultServletConfig(), configurationClasses);
  }

  public static NabApplicationContext run(ServletConfig servletConfig, Class<?>... configurationClasses) {
    configureLogger();
    NabApplicationContext context = null;
    try {
      context = new NabApplicationContext(servletConfig, configurationClasses);
      context.refresh();
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

  private static void logStartupInfo(ApplicationContext context) {
    AppMetadata appMetadata = context.getBean(AppMetadata.class);
    LOGGER.info("Started {} PID={} (version={})", appMetadata.getServiceName(), getCurrentPid(), appMetadata.getVersion());
  }

  private static String getCurrentPid() {
    return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
  }

  private static void logErrorAndExit(Exception e) {
    LOGGER.error("Failed to start, shutting down", e);
    System.err.println(format("[{0}] Failed to start, shutting down: {1}", LocalDateTime.now(), e.getMessage()));
    System.exit(1);
  }
}
