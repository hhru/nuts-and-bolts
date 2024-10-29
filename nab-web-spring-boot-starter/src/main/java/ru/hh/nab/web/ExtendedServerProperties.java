package ru.hh.nab.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(ExtendedServerProperties.PREFIX)
public class ExtendedServerProperties {

  public static final String PREFIX = "server";

  private final Jetty jetty = new Jetty();

  public Jetty getJetty() {
    return jetty;
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
