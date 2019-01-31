package ru.hh.nab.logging;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.core.Layout;
import com.papertrailapp.logback.Syslog4jAppender;
import java.util.Optional;
import org.productivity.java.syslog4j.SyslogConstants;
import org.productivity.java.syslog4j.impl.net.udp.UDPNetSyslogConfig;

public class HhSyslogAppender extends Syslog4jAppender {

  private static final int DEFAULT_MSG_LENGTH = 60000;

  @Override
  public void start() {
    if (getLayout() == null) {
      Layout<?> defaultLayout = buildDefaultLayout();
      setLayout(defaultLayout);
      getLayout().start();
    }
    String host = context.getProperty("log.syslogHost");
    String port = context.getProperty("log.syslogPort");
    UDPNetSyslogConfig udpNetSyslogConfig = new UDPNetSyslogConfig(SyslogConstants.FACILITY_USER, host, Integer.valueOf(port));
    udpNetSyslogConfig.setIdent(getName());
    //better truncate than garbage file
    udpNetSyslogConfig.setTruncateMessage(true);
    udpNetSyslogConfig.setSendLocalName(false);
    udpNetSyslogConfig.setSendLocalTimestamp(false);
    int maxMessageLength = Optional.ofNullable(context.getProperty("log.syslogMaxMessageLength")).map(length -> {
      try {
        return Integer.valueOf(length);
      } catch (NumberFormatException e) {
        return null;
      }
    }).orElse(DEFAULT_MSG_LENGTH);
    udpNetSyslogConfig.setMaxMessageLength(maxMessageLength);
    setSyslogConfig(udpNetSyslogConfig);
    super.start();
  }

  protected Layout<?> buildDefaultLayout() {
    PatternLayout patternLayout = new PatternLayout();
    patternLayout.setContext(context);
    patternLayout.setPattern(context.getProperty("log.pattern"));
    return patternLayout;
  }
}
