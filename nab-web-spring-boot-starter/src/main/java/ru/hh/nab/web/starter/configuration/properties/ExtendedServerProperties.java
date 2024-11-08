package ru.hh.nab.web.starter.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(ExtendedServerProperties.PREFIX)
public class ExtendedServerProperties {

  public static final String PREFIX = "server";

  private final Servlet servlet = new Servlet();
  private final Jetty jetty = new Jetty();

  public Servlet getServlet() {
    return servlet;
  }

  public Jetty getJetty() {
    return jetty;
  }

  public static class Servlet {

    private final Session session = new Session();
    private final Security security = new Security();

    public Session getSession() {
      return session;
    }

    public Security getSecurity() {
      return security;
    }

    public static class Session {

      private boolean enabled = true;

      public boolean isEnabled() {
        return enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }
    }

    public static class Security {

      private boolean enabled = true;

      public boolean isEnabled() {
        return enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }
    }
  }

  public static class Jetty {

    private int acceptQueueSize = 50;
    private DataSize outputBufferSize = DataSize.ofKilobytes(64);

    public int getAcceptQueueSize() {
      return acceptQueueSize;
    }

    public void setAcceptQueueSize(int acceptQueueSize) {
      this.acceptQueueSize = acceptQueueSize;
    }

    public DataSize getOutputBufferSize() {
      return outputBufferSize;
    }

    public void setOutputBufferSize(DataSize outputBufferSize) {
      this.outputBufferSize = outputBufferSize;
    }
  }
}
