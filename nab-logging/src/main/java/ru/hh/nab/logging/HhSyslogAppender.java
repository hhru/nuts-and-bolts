package ru.hh.nab.logging;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import com.papertrailapp.logback.Syslog4jAppender;
import java.util.Optional;
import org.productivity.java.syslog4j.SyslogConstants;
import org.productivity.java.syslog4j.impl.net.udp.UDPNetSyslogConfig;
import static ru.hh.nab.logging.HhMultiAppender.LOG_PATTERN_PROPERTY_KEY;

public class HhSyslogAppender extends Syslog4jAppender<ILoggingEvent> {

  private static final int DEFAULT_MSG_LENGTH = 60000;
  public static final String SYSLOG_PORT_PROPERTY_KEY = "log.syslogPort";
  public static final String SYSLOG_HOST_PROPERTY_KEY = "log.syslogHost";
  public static final String SYSLOG_MAX_MSG_LENGTH_PROPERTY_KEY = "log.syslogMaxMessageLength";

  @Override
  public void start() {
    if (getLayout() == null) {
      Layout<ILoggingEvent> defaultLayout = buildDefaultLayout();
      setLayout(defaultLayout);
      getLayout().start();
    }
    var host = context.getProperty(SYSLOG_HOST_PROPERTY_KEY);
    var port = context.getProperty(SYSLOG_PORT_PROPERTY_KEY);
    var udpNetSyslogConfig = new UDPNetSyslogConfig(SyslogConstants.FACILITY_USER, host, Integer.valueOf(port));
    udpNetSyslogConfig.setIdent(getName());
    //better truncate than garbage file
    udpNetSyslogConfig.setTruncateMessage(true);
    udpNetSyslogConfig.setSendLocalName(false);
    udpNetSyslogConfig.setSendLocalTimestamp(false);
    int maxMessageLength = Optional.ofNullable(context.getProperty(SYSLOG_MAX_MSG_LENGTH_PROPERTY_KEY)).map(length -> {
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

  protected Layout<ILoggingEvent> buildDefaultLayout() {
    PatternLayout patternLayout = new PatternLayout();
    patternLayout.setContext(context);
    patternLayout.setPattern(context.getProperty(LOG_PATTERN_PROPERTY_KEY));
    return patternLayout;
  }
}
