package ru.hh.nab.example;

import io.sentry.logback.SentryAppender;
import org.slf4j.event.Level;
import ru.hh.nab.logging.HhMultiAppender;
import ru.hh.nab.starter.NabLogbackBaseConfigurator;

public class NabExampleLogbackConfigurator extends NabLogbackBaseConfigurator {
  @Override
  public void configure(LoggingContextWrapper context, HhMultiAppender service, HhMultiAppender libraries, SentryAppender sentry) {
    getRootLogger(context).setLevel(Level.DEBUG);
  }
}
