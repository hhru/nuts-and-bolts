package ru.hh.nab.web;

import jakarta.annotation.Nullable;
import static java.util.Optional.ofNullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import ru.hh.consul.AgentClient;
import ru.hh.consul.Consul;
import ru.hh.consul.HealthClient;
import ru.hh.consul.KeyValueClient;
import ru.hh.consul.util.Address;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.profile.MainProfile;
import ru.hh.nab.starter.consul.ConsulFetcher;
import ru.hh.nab.starter.consul.ConsulMetricsTracker;
import ru.hh.nab.starter.consul.ConsulProperties;
import ru.hh.nab.starter.consul.ConsulService;
import ru.hh.nab.starter.logging.LogLevelOverrideExtension;

@Configuration
@MainProfile
@ConditionalOnProperty(name = ConsulProperties.CONSUL_ENABLED_PROPERTY, havingValue = "true", matchIfMissing = true)
public class NabConsulConfiguration {

  @Bean
  @ConfigurationProperties(ConsulProperties.PREFIX)
  @Validated
  public ConsulProperties consulProperties() {
    return new ConsulProperties();
  }

  @Bean
  public Consul consul(ConsulProperties consulProperties, InfrastructureProperties infrastructureProperties, StatsDSender statsDSender) {
    Address address = new Address(consulProperties.getHttp().getHost(), consulProperties.getHttp().getPort());
    Consul.Builder builder = Consul
        .builder()
        .withConnectTimeoutMillis(consulProperties.getClient().getConnectTimeoutMillis())
        .withReadTimeoutMillis(consulProperties.getClient().getReadTimeoutMillis())
        .withWriteTimeoutMillis(consulProperties.getClient().getWriteTimeoutMillis())
        .withAddress(address)
        .withClientEventCallback(new ConsulMetricsTracker(infrastructureProperties.getServiceName(), statsDSender))
        .withPing(consulProperties.getHttp().isPing());
    ofNullable(consulProperties.getClient().getAclToken()).ifPresent(builder::withAclToken);
    return builder.build();
  }

  @Bean
  public AgentClient agentClient(Consul consul) {
    return consul.agentClient();
  }

  @Bean
  public KeyValueClient keyValueClient(Consul consul) {
    return consul.keyValueClient();
  }

  @Bean
  public HealthClient healthClient(Consul consul) {
    return consul.healthClient();
  }

  @Bean
  public ConsulService consulService(
      InfrastructureProperties infrastructureProperties,
      BuildProperties buildProperties,
      ServerProperties serverProperties,
      ConsulProperties consulProperties,
      AgentClient agentClient,
      KeyValueClient keyValueClient,
      @Nullable LogLevelOverrideExtension logLevelOverrideExtension
  ) {
    return new ConsulService(
        agentClient,
        keyValueClient,
        infrastructureProperties.getServiceName(),
        buildProperties.getVersion(),
        infrastructureProperties.getNodeName(),
        serverProperties.getPort(),
        consulProperties,
        logLevelOverrideExtension
    );
  }

  @Bean
  public ConsulFetcher consulFetcher(HealthClient healthClient, InfrastructureProperties infrastructureProperties) {
    return new ConsulFetcher(healthClient, infrastructureProperties.getServiceName(), infrastructureProperties.getDatacenters());
  }
}
