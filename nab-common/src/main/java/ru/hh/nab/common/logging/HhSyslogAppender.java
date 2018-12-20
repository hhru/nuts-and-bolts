package ru.hh.nab.common.logging;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.Layout;
import com.papertrailapp.logback.Syslog4jAppender;
import org.productivity.java.syslog4j.SyslogConstants;
import org.productivity.java.syslog4j.impl.net.udp.UDPNetSyslogConfig;

public class HhSyslogAppender extends Syslog4jAppender {

  @Override
  public void start() {
    Layout<?> layout = getLayout();
    if (layout == null) {
      PatternLayout patternLayout = new PatternLayout();
      patternLayout.setContext(context);
      patternLayout.setPattern(context.getProperty("log.pattern"));
      setLayout(patternLayout);
      getLayout().start();

    }
    Context context = getContext();
    String host = context.getProperty("log.syslogHost");
    String port = context.getProperty("log.syslogPort");
    UDPNetSyslogConfig udpNetSyslogConfig = new UDPNetSyslogConfig(SyslogConstants.FACILITY_USER, host, Integer.valueOf(port));
    udpNetSyslogConfig.setIdent(getName());
    setSyslogConfig(udpNetSyslogConfig);
    super.start();
  }
}
