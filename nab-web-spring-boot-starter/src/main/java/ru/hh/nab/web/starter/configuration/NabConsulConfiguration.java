package ru.hh.nab.web.starter.configuration;

import java.util.Collection;
import java.util.List;
import static java.util.Optional.ofNullable;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.validation.annotation.Validated;
import ru.hh.consul.AgentClient;
import ru.hh.consul.Consul;
import ru.hh.consul.HealthClient;
import ru.hh.consul.KeyValueClient;
import ru.hh.consul.util.Address;
import ru.hh.nab.common.spring.boot.profile.MainProfile;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.web.consul.ConsulFetcher;
import ru.hh.nab.web.consul.ConsulMetricsTracker;
import ru.hh.nab.web.consul.ConsulProperties;
import ru.hh.nab.web.consul.ConsulService;
import ru.hh.nab.web.consul.ConsulTagsSupplier;
import ru.hh.nab.web.starter.configuration.properties.InfrastructureProperties;
import ru.hh.nab.web.starter.discovery.ServiceDiscoveryInitializer;

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
  public ConsulFetcher consulFetcher(HealthClient healthClient, InfrastructureProperties infrastructureProperties) {
    return new ConsulFetcher(healthClient, infrastructureProperties.getServiceName(), infrastructureProperties.getDatacenters());
  }

  @Configuration
  @Import(ServiceDiscoveryInitializer.class)
  @ConditionalOnProperty(name = ConsulProperties.CONSUL_REGISTRATION_ENABLED_PROPERTY, havingValue = "true", matchIfMissing = true)
  public static class ServiceDiscoveryConfiguration {

    @Bean
    public ConsulService consulService(
        InfrastructureProperties infrastructureProperties,
        BuildProperties buildProperties,
        ServerProperties serverProperties,
        ConsulProperties consulProperties,
        AgentClient agentClient,
        KeyValueClient keyValueClient,
        List<ConsulTagsSupplier> consulTagsSuppliers
    ) {
      Set<String> tags = Stream
          .concat(consulTagsSuppliers.stream(), Stream.of(consulProperties::getTags))
          .map(Supplier::get)
          .flatMap(Collection::stream)
          .collect(Collectors.toSet());
      return new ConsulService(
          agentClient,
          keyValueClient,
          infrastructureProperties.getServiceName(),
          buildProperties.getVersion(),
          infrastructureProperties.getNodeName(),
          serverProperties.getPort(),
          consulProperties,
          tags
      );
    }
  }
}
