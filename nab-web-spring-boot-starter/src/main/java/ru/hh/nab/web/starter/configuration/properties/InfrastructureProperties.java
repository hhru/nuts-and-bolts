package ru.hh.nab.web.starter.configuration.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@SuppressWarnings("ConfigurationProperties")
@ConfigurationProperties
@Validated
public class InfrastructureProperties {

  public static final String SERVICE_NAME_PROPERTY = "serviceName";
  public static final String NODE_NAME_PROPERTY = "nodeName";
  public static final String DATACENTER_PROPERTY = "datacenter";
  public static final String DATACENTERS_PROPERTY = "datacenters";

  @NotBlank
  private String serviceName;

  @NotBlank
  private String nodeName;

  @NotBlank
  private String datacenter;

  @NotEmpty
  private List<String> datacenters;

  private final Instant started = Instant.now();

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getNodeName() {
    return nodeName;
  }

  public void setNodeName(String nodeName) {
    this.nodeName = nodeName;
  }

  public String getDatacenter() {
    return datacenter;
  }

  public void setDatacenter(String datacenter) {
    this.datacenter = datacenter;
  }

  public List<String> getDatacenters() {
    return datacenters;
  }

  public void setDatacenters(List<String> datacenters) {
    this.datacenters = datacenters;
  }

  public Duration getUpTime() {
    return Duration.between(started, Instant.now());
  }
}
