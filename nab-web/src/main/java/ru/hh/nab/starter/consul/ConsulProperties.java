package ru.hh.nab.starter.consul;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import ru.hh.consul.option.ConsistencyMode;

public class ConsulProperties {

  public static final String PREFIX = "consul";
  public static final String CONSUL_ENABLED_PROPERTY = "consul.enabled";
  public static final String CONSUL_HTTP_PORT_PROPERTY = "consul.http.port";
  public static final String CONSUL_HTTP_HOST_PROPERTY = "consul.http.host";
  public static final String CONSUL_HTTP_PING_PROPERTY = "consul.http.ping";
  public static final String CONSUL_CHECK_HOST_PROPERTY = "consul.check.host";
  public static final String CONSUL_CHECK_INTERVAL_PROPERTY = "consul.check.interval";
  public static final String CONSUL_CHECK_TIMEOUT_PROPERTY = "consul.check.timeout";
  public static final String CONSUL_CHECK_SUCCESS_COUNT_PROPERTY = "consul.check.successCount";
  public static final String CONSUL_CHECK_FAIL_COUNT_PROPERTY = "consul.check.failCount";
  public static final String CONSUL_SERVICE_ADDRESS_PROPERTY = "consul.service.address";
  public static final String CONSUL_DEREGISTER_CRITICAL_TIMEOUT_PROPERTY = "consul.deregisterCritical.timeout";
  public static final String CONSUL_TAGS_PROPERTY = "consul.tags";

  @Valid
  private final Http http = new Http();
  private final Client client = new Client();
  private final WeightCache weightCache = new WeightCache();
  private final Service service = new Service();
  private final Check check = new Check();
  private final Registration registration = new Registration();
  private final DeregisterCritical deregisterCritical = new DeregisterCritical();

  private String consistencyMode = ConsistencyMode.DEFAULT.name();
  private int waitAfterDeregistrationMillis = 300;
  private List<String> tags = new ArrayList<>();

  public Http getHttp() {
    return http;
  }

  public Client getClient() {
    return client;
  }

  public WeightCache getWeightCache() {
    return weightCache;
  }

  public Service getService() {
    return service;
  }

  public Check getCheck() {
    return check;
  }

  public Registration getRegistration() {
    return registration;
  }

  public DeregisterCritical getDeregisterCritical() {
    return deregisterCritical;
  }

  public String getConsistencyMode() {
    return consistencyMode;
  }

  public void setConsistencyMode(String consistencyMode) {
    this.consistencyMode = consistencyMode;
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

  public static class Http {

    @NotNull
    private Integer port;
    private String host = "127.0.0.1";
    private boolean ping = true;

    public Integer getPort() {
      return port;
    }

    public void setPort(Integer port) {
      this.port = port;
    }

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public boolean isPing() {
      return ping;
    }

    public void setPing(boolean ping) {
      this.ping = ping;
    }
  }

  public static class Client {

    private long connectTimeoutMillis = 10_500;
    private long readTimeoutMillis = 10_500;
    private long writeTimeoutMillis = 10_500;
    private String aclToken;

    public long getConnectTimeoutMillis() {
      return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(long connectTimeoutMillis) {
      this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public long getReadTimeoutMillis() {
      return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(long readTimeoutMillis) {
      this.readTimeoutMillis = readTimeoutMillis;
    }

    public long getWriteTimeoutMillis() {
      return writeTimeoutMillis;
    }

    public void setWriteTimeoutMillis(long writeTimeoutMillis) {
      this.writeTimeoutMillis = writeTimeoutMillis;
    }

    public String getAclToken() {
      return aclToken;
    }

    public void setAclToken(String aclToken) {
      this.aclToken = aclToken;
    }
  }

  public static class WeightCache {

    private String consistencyMode;
    private int watchSeconds = 10;

    public String getConsistencyMode() {
      return consistencyMode;
    }

    public void setConsistencyMode(String consistencyMode) {
      this.consistencyMode = consistencyMode;
    }

    public int getWatchSeconds() {
      return watchSeconds;
    }

    public void setWatchSeconds(int watchSeconds) {
      this.watchSeconds = watchSeconds;
    }
  }

  public static class Service {

    private String address;

    public String getAddress() {
      return address;
    }

    public void setAddress(String address) {
      this.address = address;
    }
  }

  public static class Check {

    private String host;
    private Boolean passing;
    private String interval = "5s";
    private String timeout = "5s";
    private int successCount = 1;
    private int failCount = 1;

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public Boolean getPassing() {
      return passing;
    }

    public void setPassing(Boolean passing) {
      this.passing = passing;
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
  }

  public static class Registration {

    private boolean enabled = true;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }

  public static class DeregisterCritical {

    private String timeout = "10m";

    public String getTimeout() {
      return timeout;
    }

    public void setTimeout(String timeout) {
      this.timeout = timeout;
    }
  }
}
