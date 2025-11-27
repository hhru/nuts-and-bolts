package ru.hh.nab.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LoggingUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingUncaughtExceptionHandler.class);

  private static Thread.UncaughtExceptionHandler replacedDefaultExceptionHandler;

  public static void registerAsDefault() {
    replacedDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    Thread.setDefaultUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler());
  }

  @Override
  public void uncaughtException(Thread t, Throwable e) {
    LOGGER.error("Uncaught exception in {}", t, e);
    if (replacedDefaultExceptionHandler != null) {
      replacedDefaultExceptionHandler.uncaughtException(t, e);
    }
  }

  private LoggingUncaughtExceptionHandler() {
  }
}
