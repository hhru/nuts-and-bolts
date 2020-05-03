package ru.hh.nab.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class ListAppenderTest {
  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ListAppenderTest.class);

  private ListAppender listAppender;

  @BeforeEach
  public void setUp() {
    listAppender = new ListAppender();
    listAppender.start();

    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.addAppender(listAppender);
  }

  @AfterEach
  public void tearDown() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.detachAppender(listAppender);

    listAppender.stop();
  }

  @Test
  public void testName() {
    LOGGER.info("something important");

    String actualLogLine = listAppender.getLogLineBySubstring("something");
    assertEquals("something important", actualLogLine);
  }
}

