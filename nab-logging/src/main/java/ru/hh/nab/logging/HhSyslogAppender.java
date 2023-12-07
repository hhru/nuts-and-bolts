package ru.hh.nab.logging;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import com.papertrailapp.logback.Syslog4jAppender;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.productivity.java.syslog4j.SyslogConstants;
import org.productivity.java.syslog4j.impl.net.udp.UDPNetSyslogConfig;
import static ru.hh.nab.logging.HhMultiAppender.LOG_PATTERN_PROPERTY_KEY;

public class HhSyslogAppender extends Syslog4jAppender<ILoggingEvent> {

  private static final int DEFAULT_MSG_LENGTH = 60000;
  public static final String SYSLOG_HOST_ENV = "SYSLOG_HOST";
  public static final String SYSLOG_PORT_PROPERTY_KEY = "log.syslogPort";
  public static final String SYSLOG_HOST_PROPERTY_KEY = "log.syslogHost";
  public static final String SYSLOG_MAX_MSG_LENGTH_PROPERTY_KEY = "log.syslogMaxMessageLength";
  public static final String SYSLOG_TAG = "log.syslogTag";
  private static final String SYSLOG_DELIMITER = "/";

  private final boolean json;

  public HhSyslogAppender(boolean json) {
    super();
    this.json = json;
  }

  @Override
  public void start() {
    if (getLayout() == null) {
      Layout<ILoggingEvent> defaultLayout = buildDefaultLayout();
      setLayout(defaultLayout);
      getLayout().start();
    }
    var host = Optional.ofNullable(System.getenv(SYSLOG_HOST_ENV)).orElseGet(() -> context.getProperty(SYSLOG_HOST_PROPERTY_KEY));
    var port = context.getProperty(SYSLOG_PORT_PROPERTY_KEY);
    var tag = context.getProperty(SYSLOG_TAG);
    var udpNetSyslogConfig = new UDPNetSyslogConfig(SyslogConstants.FACILITY_USER, host, Integer.parseInt(port));
    udpNetSyslogConfig.setIdent(generateIdent(tag));
    //better truncate than garbage file
    udpNetSyslogConfig.setTruncateMessage(true);
    udpNetSyslogConfig.setSendLocalName(false);
    udpNetSyslogConfig.setSendLocalTimestamp(false);
    int maxMessageLength = Optional
        .ofNullable(context.getProperty(SYSLOG_MAX_MSG_LENGTH_PROPERTY_KEY))
        .map(length -> {
          try {
            return Integer.valueOf(length);
          } catch (NumberFormatException e) {
            return null;
          }
        })
        .orElse(DEFAULT_MSG_LENGTH);
    udpNetSyslogConfig.setMaxMessageLength(maxMessageLength);
    setSyslogConfig(udpNetSyslogConfig);
    super.start();
  }

  private String generateIdent(String tag) {
    return (StringUtils.isNotEmpty(tag) ? tag + SYSLOG_DELIMITER : "") + getName() + (json ? ".slog" : ".rlog") + SYSLOG_DELIMITER;
  }

  protected Layout<ILoggingEvent> buildDefaultLayout() {
    PatternLayout patternLayout = new PatternLayout();
    patternLayout.setContext(context);
    patternLayout.setPattern(context.getProperty(LOG_PATTERN_PROPERTY_KEY));
    return patternLayout;
  }
}
