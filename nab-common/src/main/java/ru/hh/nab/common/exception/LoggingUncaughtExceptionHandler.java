package ru.hh.nab.common.exception;

import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LoggingUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingUncaughtExceptionHandler.class);

  private static volatile Thread.UncaughtExceptionHandler replacedDefaultExceptionHandler;
  private static final AtomicBoolean registered = new AtomicBoolean(false);

  public static void registerAsDefault() {
    if (registered.compareAndSet(false, true)) {
      replacedDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
      Thread.setDefaultUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler());
    }
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
