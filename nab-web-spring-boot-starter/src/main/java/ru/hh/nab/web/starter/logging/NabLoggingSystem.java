package ru.hh.nab.web.starter.logging;

import java.util.Collections;
import java.util.List;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.LoggingSystemFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

// TODO: https://jira.hh.ru/browse/PORTFOLIO-33636
public class NabLoggingSystem extends LoggingSystem {

  @Override
  public void beforeInitialize() {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  @Override
  public void setLogLevel(String loggerName, LogLevel level) {
  }

  @Override
  public List<LoggerConfiguration> getLoggerConfigurations() {
    return Collections.emptyList();
  }

  @Override
  public LoggerConfiguration getLoggerConfiguration(String loggerName) {
    return null;
  }

  @Order(Ordered.HIGHEST_PRECEDENCE)
  public static class Factory implements LoggingSystemFactory {

    @Override
    public LoggingSystem getLoggingSystem(ClassLoader classLoader) {
      return new NabLoggingSystem();
    }
  }
}
