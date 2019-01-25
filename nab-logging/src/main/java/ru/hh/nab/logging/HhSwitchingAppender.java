package ru.hh.nab.logging;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import org.apache.commons.lang3.StringUtils;

public class HhSwitchingAppender<E> extends AppenderBase<E> {

  protected Appender appender;
  protected Layout<?> layout;

  @Override
  public void start() {
    Context context = getContext();
    String port = context.getProperty("log.syslogPort");
    if (StringUtils.isNumeric(port)) {
      HhSyslogAppender appender = new HhSyslogAppender();
      appender.setLayout(getLayout());
      this.appender = appender;
    } else {
      HhRollingAppender appender = new HhRollingAppender();
      LayoutWrappingEncoder layoutWrappingEncoder = new LayoutWrappingEncoder();
      layoutWrappingEncoder.setLayout(getLayout());
      appender.setEncoder(layoutWrappingEncoder);
      this.appender = appender;
    }
    appender.setName(getName());
    appender.setContext(getContext());
    appender.start();
    super.start();
  }

  @Override
  public synchronized void doAppend(E eventObject) {
    appender.doAppend(eventObject);
  }

  @Override
  protected void append(E eventObject) {
    throw new UnsupportedOperationException("method should never be called");
  }

  public Layout<?> getLayout() {
    return layout;
  }

  public void setLayout(Layout<?> layout) {
    this.layout = layout;
  }
}
