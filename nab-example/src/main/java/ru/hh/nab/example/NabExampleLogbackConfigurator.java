package ru.hh.nab.example;

import org.slf4j.event.Level;
import ru.hh.nab.logging.HhMultiAppender;
import ru.hh.nab.starter.NabLogbackBaseConfigurator;

public class NabExampleLogbackConfigurator extends NabLogbackBaseConfigurator {
  @Override
  public void configure(LoggingContextWrapper context, HhMultiAppender service, HhMultiAppender libraries) {
    getRootLogger(context).setLevel(Level.DEBUG);
  }
}
