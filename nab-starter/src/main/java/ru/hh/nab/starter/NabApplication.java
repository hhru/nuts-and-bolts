package ru.hh.nab.starter;

import static java.text.MessageFormat.format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.context.ApplicationContext;
import ru.hh.nab.starter.servlet.DefaultServletConfig;
import ru.hh.nab.starter.servlet.ServletConfig;

import java.time.LocalDateTime;

public class NabApplication {
  private static final Logger LOGGER = LoggerFactory.getLogger(NabApplication.class);

  public static ApplicationContext run(Class<?>... primarySources) {
    return run(new DefaultServletConfig(), primarySources);
  }

  public static ApplicationContext run(ServletConfig servletConfig, Class<?>... primarySources) {
    configureLogger();
    NabApplicationContext context = null;
    try {
      context = new NabApplicationContext(servletConfig, primarySources);
      context.refresh();
    } catch (Exception e) {
      logErrorAndExit(e);
    }
    return context;
  }

  public static void configureLogger() {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  private static void logErrorAndExit(Exception e) {
    LOGGER.error("Failed to start, shutting down", e);
    System.err.println(format("[{0}] Failed to start, shutting down: {1}", LocalDateTime.now(), e.getMessage()));
    System.exit(1);
  }
}
