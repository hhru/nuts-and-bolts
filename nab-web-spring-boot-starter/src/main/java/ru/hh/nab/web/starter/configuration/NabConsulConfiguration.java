package ru.hh.nab.web.starter.configuration;

import java.util.HashSet;
import java.util.List;
import static java.util.Optional.ofNullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import ru.hh.consul.AgentClient;
import ru.hh.consul.Consul;
import ru.hh.consul.HealthClient;
import ru.hh.consul.KeyValueClient;
import ru.hh.consul.util.Address;
import ru.hh.nab.common.spring.boot.env.EnvironmentUtils;
import ru.hh.nab.common.spring.boot.profile.MainProfile;
import ru.hh.nab.consul.ConsulFetcher;
import ru.hh.nab.consul.ConsulMetricsTracker;
import ru.hh.nab.consul.ConsulService;
import ru.hh.nab.consul.ConsulTagsSupplier;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.web.starter.configuration.properties.InfrastructureProperties;
import ru.hh.nab.web.starter.discovery.ServiceDiscoveryInitializer;

@Configuration
@MainProfile
@ConditionalOnProperty(name = NabConsulConfiguration.CONSUL_ENABLED_PROPERTY, havingValue = "true", matchIfMissing = true)
public class NabConsulConfiguration {

  public static final String CONSUL_ENABLED_PROPERTY = "consul.enabled";
  public static final String CONSUL_REGISTRATION_ENABLED_PROPERTY = "consul.registration.enabled";
  public static final String CONSUL_HTTP_PORT_PROPERTY = "consul.http.port";
  public static final String CONSUL_HTTP_HOST_PROPERTY = "consul.http.host";
  public static final String CONSUL_HTTP_PING_PROPERTY = "consul.http.ping";
  public static final String CONSUL_CLIENT_CONNECT_TIMEOUT_PROPERTY = "consul.client.connectTimeoutMillis";
  public static final String CONSUL_CLIENT_READ_TIMEOUT_PROPERTY = "consul.client.readTimeoutMillis";
  public static final String CONSUL_CLIENT_WRITE_TIMEOUT_PROPERTY = "consul.client.writeTimeoutMillis";
  public static final String CONSUL_CLIENT_ACL_TOKEN_PROPERTY = "consul.client.aclToken";
  public static final String CONSUL_TAGS_PROPERTY = "consul.tags";

  public static final String DEFAULT_HTTP_HOST = "127.0.0.1";
  public static final long DEFAULT_READ_TIMEOUT_MILLIS = 10_500L;
  public static final long DEFAULT_WRITE_TIMEOUT_MILLIS = 10_500L;
  public static final long DEFAULT_CONNECT_TIMEOUT_MILLIS = 10_500L;
  public static final boolean DEFAULT_HTTP_PING = true;

  @Bean
  public Consul consul(Environment environment, InfrastructureProperties infrastructureProperties, StatsDSender statsDSender) {
    Address address = new Address(
        environment.getProperty(CONSUL_HTTP_HOST_PROPERTY, DEFAULT_HTTP_HOST),
        environment.getRequiredProperty(CONSUL_HTTP_PORT_PROPERTY, Integer.class)
    );
    Consul.Builder builder = Consul
        .builder()
        .withConnectTimeoutMillis(environment.getProperty(CONSUL_CLIENT_CONNECT_TIMEOUT_PROPERTY, Long.class, DEFAULT_CONNECT_TIMEOUT_MILLIS))
        .withReadTimeoutMillis(environment.getProperty(CONSUL_CLIENT_READ_TIMEOUT_PROPERTY, Long.class, DEFAULT_READ_TIMEOUT_MILLIS))
        .withWriteTimeoutMillis(environment.getProperty(CONSUL_CLIENT_WRITE_TIMEOUT_PROPERTY, Long.class, DEFAULT_WRITE_TIMEOUT_MILLIS))
        .withAddress(address)
        .withClientEventCallback(new ConsulMetricsTracker(infrastructureProperties.getServiceName(), statsDSender))
        .withPing(environment.getProperty(CONSUL_HTTP_PING_PROPERTY, Boolean.class, DEFAULT_HTTP_PING));
    ofNullable(environment.getProperty(CONSUL_CLIENT_ACL_TOKEN_PROPERTY)).ifPresent(builder::withAclToken);
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
  @ConditionalOnProperty(name = NabConsulConfiguration.CONSUL_REGISTRATION_ENABLED_PROPERTY, havingValue = "true", matchIfMissing = true)
  public static class ServiceDiscoveryConfiguration {

    @Bean
    public ConsulService consulService(
        InfrastructureProperties infrastructureProperties,
        BuildProperties buildProperties,
        ServerProperties serverProperties,
        ConfigurableEnvironment environment,
        AgentClient agentClient,
        KeyValueClient keyValueClient,
        List<ConsulTagsSupplier> consulTagsSuppliers
    ) {
      HashSet<String> tags = new HashSet<>();
      consulTagsSuppliers.forEach(consulTagsSupplier -> tags.addAll(consulTagsSupplier.get()));
      tags.addAll(EnvironmentUtils.getPropertyAsStringList(environment, CONSUL_TAGS_PROPERTY));
      return new ConsulService(
          agentClient,
          keyValueClient,
          infrastructureProperties.getServiceName(),
          buildProperties.getVersion(),
          infrastructureProperties.getNodeName(),
          serverProperties.getPort(),
          EnvironmentUtils.getProperties(environment),
          tags
      );
    }
  }
}
