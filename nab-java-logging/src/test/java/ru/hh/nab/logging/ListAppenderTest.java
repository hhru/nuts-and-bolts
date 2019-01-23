package ru.hh.nab.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class ListAppenderTest {
  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ListAppenderTest.class);

  private ListAppender listAppender;

  @Before
  public void setUp() {
    listAppender = new ListAppender();
    listAppender.start();

    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.addAppender(listAppender);
  }

  @After
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

