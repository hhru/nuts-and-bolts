package ru.hh.nab.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.LevelToSyslogSeverity;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import java.lang.reflect.InvocationTargetException;
import org.productivity.java.syslog4j.SyslogConfigIF;
import org.productivity.java.syslog4j.SyslogIF;
import org.productivity.java.syslog4j.SyslogRuntimeException;

public class UnsynchronizedSyslog4jAppender<E> extends UnsynchronizedAppenderBase<E> {

  private SyslogIF syslog;
  private SyslogConfigIF syslogConfig;
  private Layout<E> layout;

  @Override
  protected void append(E loggingEvent) {
    syslog.log(getSeverityForEvent(loggingEvent), layout.doLayout(loggingEvent));
  }

  @Override
  public void start() {
    super.start();

    synchronized (this) {
      try {
        Class syslogClass = syslogConfig.getSyslogClass();
        syslog = (SyslogIF) syslogClass.getDeclaredConstructor().newInstance();

        syslog.initialize(syslogClass.getSimpleName(), syslogConfig);
      } catch (ClassCastException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        throw new SyslogRuntimeException(e);
      }
    }
  }

  @Override
  public void stop() {
    super.stop();

    synchronized (this) {
      if (syslog != null) {
        syslog.shutdown();
        syslog = null;
      }
    }
  }

  public int getSeverityForEvent(Object eventObject) {
    if (eventObject instanceof ILoggingEvent event) {
      return LevelToSyslogSeverity.convert(event);
    } else {
      return SyslogIF.LEVEL_INFO;
    }
  }

  public void setSyslogConfig(SyslogConfigIF syslogConfig) {
    this.syslogConfig = syslogConfig;
  }

  public Layout<E> getLayout() {
    return layout;
  }

  public void setLayout(Layout<E> layout) {
    this.layout = layout;
  }
}
