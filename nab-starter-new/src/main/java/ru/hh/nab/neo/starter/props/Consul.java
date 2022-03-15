package ru.hh.nab.neo.starter.props;

import java.util.ArrayList;
import java.util.List;
import ru.hh.consul.option.ConsistencyMode;

public class Consul {
  private final Http http = new Http();
  private final Client client = new Client();
  private final Check check = new Check();
  private final WeightCache weightCache = new WeightCache();
  private String serviceAddress;
  private String resolve;
  private int waitAfterDeregistrationMillis = 300;
  private List<String> tags = new ArrayList<>();
  private boolean enabled = true;
  private String consistencyMode = ConsistencyMode.DEFAULT.name();
  private boolean registrationEnabled = true;
  private String deregisterCriticalTimeout = "10m";

  public String getServiceAddress() {
    return serviceAddress;
  }

  public void setServiceAddress(String serviceAddress) {
    this.serviceAddress = serviceAddress;
  }

  public String getResolve() {
    return resolve;
  }

  public void setResolve(String resolve) {
    this.resolve = resolve;
  }

  public int getWaitAfterDeregistrationMillis() {
    return waitAfterDeregistrationMillis;
  }

  public void setWaitAfterDeregistrationMillis(int waitAfterDeregistrationMillis) {
    this.waitAfterDeregistrationMillis = waitAfterDeregistrationMillis;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getConsistencyMode() {
    return consistencyMode;
  }

  public void setConsistencyMode(String consistencyMode) {
    this.consistencyMode = consistencyMode;
  }

  public boolean getRegistrationEnabled() {
    return registrationEnabled;
  }

  public void setRegistrationEnabled(boolean registrationEnabled) {
    this.registrationEnabled = registrationEnabled;
  }

  public String getDeregisterCriticalTimeout() {
    return deregisterCriticalTimeout;
  }

  public void setDeregisterCriticalTimeout(String deregisterCriticalTimeout) {
    this.deregisterCriticalTimeout = deregisterCriticalTimeout;
  }

  public WeightCache getWeightCache() {
    return weightCache;
  }

  public Http getHttp() {
    return http;
  }

  public Client getClient() {
    return client;
  }

  public Check getCheck() {
    return check;
  }

  public static class Http {
    private String host = "127.0.0.1";
    private Integer port;

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public Integer getPort() {
      return port;
    }

    public void setPort(Integer port) {
      this.port = port;
    }
  }

  public static class Client {
    private int connectTimeoutMillis = 10_500;
    private int readTimeoutMillis = 10_500;
    private int writeTimeoutMillis = 10_500;
    private String aclToken;

    public int getConnectTimeoutMillis() {
      return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
      this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public int getReadTimeoutMillis() {
      return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(int readTimeoutMillis) {
      this.readTimeoutMillis = readTimeoutMillis;
    }

    public int getWriteTimeoutMillis() {
      return writeTimeoutMillis;
    }

    public void setWriteTimeoutMillis(int writeTimeoutMillis) {
      this.writeTimeoutMillis = writeTimeoutMillis;
    }

    public String getAclToken() {
      return aclToken;
    }

    public void setAclToken(String aclToken) {
      this.aclToken = aclToken;
    }
  }

  public static class Check {
    private String host = "127.0.0.1";
    private String interval = "5s";
    private String timeout = "5s";
    private int successCount = 1;
    private int failCount = 1;
    private Boolean passing;

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public String getInterval() {
      return interval;
    }

    public void setInterval(String interval) {
      this.interval = interval;
    }

    public String getTimeout() {
      return timeout;
    }

    public void setTimeout(String timeout) {
      this.timeout = timeout;
    }

    public int getSuccessCount() {
      return successCount;
    }

    public void setSuccessCount(int successCount) {
      this.successCount = successCount;
    }

    public int getFailCount() {
      return failCount;
    }

    public void setFailCount(int failCount) {
      this.failCount = failCount;
    }

    public Boolean getPassing() {
      return passing;
    }

    public void setPassing(Boolean passing) {
      this.passing = passing;
    }
  }

  public static class WeightCache {
    private int watchSeconds = 10;
    private String consistencyMode;

    public int getWatchSeconds() {
      return watchSeconds;
    }

    public void setWatchSeconds(int watchSeconds) {
      this.watchSeconds = watchSeconds;
    }

    public String getConsistencyMode() {
      return consistencyMode;
    }

    public void setConsistencyMode(String consistencyMode) {
      this.consistencyMode = consistencyMode;
    }
  }
}
