package ru.hh.nab.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Context;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class HhSyslogAppenderTest {

  @Test
  public void testLoadPattern() throws Exception {
    Function<Context, HhSyslogAppender> hhSyslogAppenderFunction = context -> {
      HhSyslogAppender appender = new HhSyslogAppender();
      appender.setContext(context);
      return appender;
    };
    LocalDateTime epochStart = LocalDateTime.ofEpochSecond(0, 0, OffsetDateTime.now().getOffset());
    testLogging(hhSyslogAppenderFunction, "test", "<11>test: ["
      + epochStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS"))
      + "] ERROR logger:1 mdc={} - message");
  }

  @Test
  public void testLayout() throws Exception {
    Function<Context, HhSyslogAppender> hhSyslogAppenderFunction = context -> {
      HhSyslogAppender appender = new HhSyslogAppender();
      appender.setContext(context);
      PatternLayout patternLayout = new PatternLayout();
      patternLayout.setContext(context);
      appender.setLayout(patternLayout);
      patternLayout.setPattern("%msg%n");
      patternLayout.start();
      return appender;
    };
    testLogging(hhSyslogAppenderFunction, "test", "<11>test: message");
  }

  protected void testLogging(Function<Context, HhSyslogAppender> appenderCreateFunction, String pid, String expected) throws Exception {
    DatagramSocket serverSocket = new DatagramSocket();
    LoggerContext context = new LoggerContext();
    context.putProperty("log.syslogHost", "localhost");
    context.putProperty("log.syslogPort", String.valueOf(serverSocket.getLocalPort()));
    context.putProperty("log.pattern", "[%date{ISO8601}] %-5level %logger{36}:%line mdc={%mdc} - %msg%n");
    HhSyslogAppender appender = appenderCreateFunction.apply(context);
    appender.setName(pid);
    appender.start();
    Exchanger<String> exchanger = new Exchanger<>();
    Thread checker = new Thread(() -> {
      byte[] receiveData = new byte[1024];
      try {
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        serverSocket.receive(receivePacket);
        String message = new String(receivePacket.getData()).trim();
        exchanger.exchange(message);
      } catch (IOException | InterruptedException e) {
        throw new RuntimeException(e);
      }
    });
    checker.start();
    LoggingEvent eventObject = new LoggingEvent();
    eventObject.setCallerData(new StackTraceElement[] {new StackTraceElement("class", "method", null, 1)});
    eventObject.setMDCPropertyMap(Collections.emptyMap());
    eventObject.setLevel(Level.ERROR);
    eventObject.setTimeStamp(0);
    eventObject.setMessage("message");
    eventObject.setLoggerName("logger");
    appender.doAppend(eventObject);
    String message = exchanger.exchange(null, 5, TimeUnit.SECONDS);
    assertEquals(expected, message);
  }

}
