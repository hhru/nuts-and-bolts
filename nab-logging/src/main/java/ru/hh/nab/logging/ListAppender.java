package ru.hh.nab.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class ListAppender extends ch.qos.logback.core.read.ListAppender<ILoggingEvent> {

  public synchronized String getLogLineBySubstring(String substring) {
    return list.stream().map(ILoggingEvent::getFormattedMessage).filter(m -> m.contains(substring)).findFirst().orElse(null);
  }
}

