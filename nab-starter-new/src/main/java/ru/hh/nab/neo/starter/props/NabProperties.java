package ru.hh.nab.neo.starter.props;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "nab")
public class NabProperties {
  private final Server server = new Server();
  private final Metrics metrics = new Metrics();
  @NestedConfigurationProperty
  private final Consul consul = new Consul();
  /**
   * Name of the service.
   */
  private String serviceName;
  private String datacenter;
  private String nodeName;

  public String getServiceName() {
    return this.serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getDatacenter() {
    return this.datacenter;
  }

  public void setDatacenter(String datacenter) {
    this.datacenter = datacenter;
  }

  public String getNodeName() {
    return this.nodeName;
  }

  public void setNodeName(String nodeName) {
    this.nodeName = nodeName;
  }

  public Server getServer() {
    return this.server;
  }

  public Metrics getMetrics() {
    return this.metrics;
  }

  public Consul getConsul() {
    return consul;
  }

  public static class Server {
    private final Threads threads = new Threads();
    private int securePort = 8443;
    private DataSize outputBufferSize = DataSize.ofBytes(65536);
    private DataSize responseHeaderSize = DataSize.ofBytes(65536);
    private int stopTimeoutMs = 5000;

    public int getSecurePort() {
      return securePort;
    }

    public void setSecurePort(int securePort) {
      this.securePort = securePort;
    }

    public DataSize getOutputBufferSize() {
      return outputBufferSize;
    }

    public void setOutputBufferSize(DataSize outputBufferSize) {
      this.outputBufferSize = outputBufferSize;
    }

    public DataSize getResponseHeaderSize() {
      return responseHeaderSize;
    }

    public void setResponseHeaderSize(DataSize responseHeaderSize) {
      this.responseHeaderSize = responseHeaderSize;
    }

    public int getStopTimeoutMs() {
      return this.stopTimeoutMs;
    }

    public void setStopTimeoutMs(int stopTimeoutMs) {
      this.stopTimeoutMs = stopTimeoutMs;
    }

    public Threads getThreads() {
      return threads;
    }

    public static class Threads {
//      private int maxThreads = 12;
//      private int minThreads = maxThreads;
//      private int queueSize = maxThreads;
      private Integer queueSize;
      private int threadPoolIdleTimeoutMs = (int) Duration.ofMinutes(1).toMillis();

//      public int getMaxThreads() {
//        return maxThreads;
//      }
//
//      public void setMaxThreads(int maxThreads) {
//        this.maxThreads = maxThreads;
//      }
//
//      public int getMinThreads() {
//        return minThreads;
//      }
//
//      public void setMinThreads(int minThreads) {
//        this.minThreads = minThreads;
//      }

      public Integer getQueueSize() {
        return queueSize;
      }

      public void setQueueSize(Integer queueSize) {
        this.queueSize = queueSize;
      }

      public int getThreadPoolIdleTimeoutMs() {
        return threadPoolIdleTimeoutMs;
      }

      public void setThreadPoolIdleTimeoutMs(int threadPoolIdleTimeoutMs) {
        this.threadPoolIdleTimeoutMs = threadPoolIdleTimeoutMs;
      }
    }
  }

  public static class Metrics {
    private boolean jvmEnabled;

    public boolean isJvmEnabled() {
      return jvmEnabled;
    }

    public void setJvmEnabled(boolean jvmEnabled) {
      this.jvmEnabled = jvmEnabled;
    }
  }
}
