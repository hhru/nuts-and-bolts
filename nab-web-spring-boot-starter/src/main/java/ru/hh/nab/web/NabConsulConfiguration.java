package ru.hh.nab.web;

import jakarta.annotation.Nullable;
import jakarta.inject.Named;
import java.util.List;
import java.util.Objects;
import static java.util.Objects.requireNonNullElse;
import static java.util.Optional.ofNullable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import ru.hh.consul.AgentClient;
import ru.hh.consul.Consul;
import ru.hh.consul.HealthClient;
import ru.hh.consul.KeyValueClient;
import ru.hh.consul.util.Address;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.qualifier.NamedQualifier.DATACENTERS;
import static ru.hh.nab.common.qualifier.NamedQualifier.NODE_NAME;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.profile.MainProfile;
import ru.hh.nab.starter.AppMetadata;
import ru.hh.nab.starter.consul.ConsulFetcher;
import ru.hh.nab.starter.consul.ConsulMetricsTracker;
import ru.hh.nab.starter.consul.ConsulService;
import ru.hh.nab.starter.logging.LogLevelOverrideExtension;

@Configuration
@MainProfile
public class NabConsulConfiguration {

  private static final String CONSUL_PORT_PROPERTY = "consul.http.port";
  private static final String CONSUL_HOST_PROPERTY = "consul.http.host";
  private static final String CONSUL_PING_PROPERTY = "consul.http.ping";
  private static final String CONSUL_CLIENT_CONNECT_TIMEOUT_PROPERTY = "consul.client.connectTimeoutMillis";
  private static final String CONSUL_CLIENT_READ_TIMEOUT_PROPERTY = "consul.client.readTimeoutMillis";
  private static final String CONSUL_CLIENT_WRITE_TIMEOUT_PROPERTY = "consul.client.writeTimeoutMillis";
  private static final String CONSUL_CLIENT_ACL_TOKEN = "consul.client.aclToken";
  private static final int CONSUL_DEFAULT_READ_TIMEOUT_MILLIS = 10_500;

  @Bean
  public Consul consul(FileSettings fileSettings, @Named(SERVICE_NAME) String serviceName, StatsDSender statsDSender) {
    if (isConsulDisabled(fileSettings)) {
      return null;
    }

    int port = ofNullable(fileSettings.getString(CONSUL_PORT_PROPERTY))
        .or(() -> ofNullable(System.getProperty(CONSUL_PORT_PROPERTY)))
        .map(Integer::parseInt)
        .orElseThrow(() -> new IllegalStateException(CONSUL_PORT_PROPERTY + " setting or property be provided"));
    Address address = new Address(requireNonNullElse(fileSettings.getString(CONSUL_HOST_PROPERTY), "127.0.0.1"), port);
    Consul.Builder builder = Consul
        .builder()
        .withConnectTimeoutMillis(fileSettings.getLong(CONSUL_CLIENT_CONNECT_TIMEOUT_PROPERTY, 10_500))
        .withReadTimeoutMillis(fileSettings.getLong(CONSUL_CLIENT_READ_TIMEOUT_PROPERTY, CONSUL_DEFAULT_READ_TIMEOUT_MILLIS))
        .withWriteTimeoutMillis(fileSettings.getLong(CONSUL_CLIENT_WRITE_TIMEOUT_PROPERTY, 10_500))
        .withAddress(address);
    builder.withClientEventCallback(new ConsulMetricsTracker(serviceName, statsDSender));
    return ofNullable(fileSettings.getString(CONSUL_CLIENT_ACL_TOKEN))
        .map(builder::withAclToken)
        .orElse(builder)
        .withPing(fileSettings.getBoolean(CONSUL_PING_PROPERTY, true))
        .build();
  }

  @Bean
  public AgentClient agentClient(@Nullable Consul consul) {
    return consul != null ? consul.agentClient() : null;
  }

  @Bean
  public KeyValueClient keyValueClient(@Nullable Consul consul) {
    return consul != null ? consul.keyValueClient() : null;
  }

  @Bean
  public HealthClient healthClient(@Nullable Consul consul) {
    return consul != null ? consul.healthClient() : null;
  }

  @Bean
  @Lazy(value = false)
  public ConsulService consulService(
      FileSettings fileSettings,
      AppMetadata appMetadata,
      @Nullable AgentClient agentClient,
      @Nullable KeyValueClient keyValueClient,
      @Named(NODE_NAME) String nodeName,
      @Nullable LogLevelOverrideExtension logLevelOverrideExtension
  ) {
    if (isConsulDisabled(fileSettings)) {
      return null;
    }
    return new ConsulService(
        Objects.requireNonNull(agentClient),
        Objects.requireNonNull(keyValueClient),
        fileSettings,
        appMetadata,
        nodeName,
        logLevelOverrideExtension
    );
  }

  @Bean
  @Lazy
  public ConsulFetcher consulFetcher(
      @Nullable HealthClient healthClient,
      @Named(SERVICE_NAME) String serviceName,
      @Named(DATACENTERS) List<String> datacenters
  ) {
    if (healthClient == null) {
      throw new RuntimeException(String.format("HealthClient is null. Set %s as true for using fetcher", ConsulService.CONSUL_ENABLED_PROPERTY));
    }

    return new ConsulFetcher(healthClient, serviceName, datacenters);
  }

  private boolean isConsulDisabled(FileSettings fileSettings) {
    return !fileSettings.getBoolean(ConsulService.CONSUL_ENABLED_PROPERTY, true);
  }
}
