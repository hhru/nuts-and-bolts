package ru.hh.nab.neo.starter;

import com.timgroup.statsd.NonBlockingStatsDClientBuilder;
import com.timgroup.statsd.StatsDClient;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import ru.hh.consul.AgentClient;
import ru.hh.consul.Consul;
import ru.hh.consul.KeyValueClient;
import ru.hh.consul.util.Address;
import ru.hh.nab.neo.starter.events.JettyEventListener;
import ru.hh.nab.neo.starter.logging.LogLevelOverrideExtension;
import ru.hh.nab.neo.starter.props.NabProperties;

@Configuration
@Import({NabCommonConfig.class})
public class NabProdConfig {

  @Bean
  StatsDClient statsDClient() {
    return new NonBlockingStatsDClientBuilder().hostname("localhost").queueSize(10_000).port(8125).build();
  }

  @Bean
  @ConditionalOnProperty("nab.consul.enabled")
  Consul consul(NabProperties nabProperties) {
    ru.hh.nab.neo.starter.props.Consul consulProperties = nabProperties.getConsul();
    int port = ofNullable(consulProperties.getHttp().getPort())
        .orElseThrow(() -> new IllegalStateException("nab.consul.http.port setting or property be provided"));
    Address address = new Address(consulProperties.getHttp().getHost(), port);
    Consul.Builder builder = Consul.builder()
        .withConnectTimeoutMillis(consulProperties.getClient().getConnectTimeoutMillis())
        .withReadTimeoutMillis(consulProperties.getClient().getReadTimeoutMillis())
        .withWriteTimeoutMillis(consulProperties.getClient().getWriteTimeoutMillis())
        .withAddress(address);
    return ofNullable(consulProperties.getClient().getAclToken()).map(builder::withAclToken).orElse(builder).build();
  }

  @Bean
  AgentClient agentClient(Optional<Consul> consul) {
    return consul.map(Consul::agentClient).orElse(null);
  }

  @Bean
  KeyValueClient keyValueClient(Optional<Consul> consul) {
    return consul.map(Consul::keyValueClient).orElse(null);
  }

  @Bean
  @Lazy(value = false)
  @ConditionalOnProperty("nab.consul.enabled")
  ConsulService consulService(NabProperties nabProperties,
                              ServerProperties serverProperties,
                              AppMetadata appMetadata,
                              Optional<AgentClient> agentClient,
                              Optional<KeyValueClient> keyValueClient,
                              Optional<LogLevelOverrideExtension> logLevelOverrideExtensionOptional) {
    return new ConsulService(
        agentClient.orElseThrow(),
        keyValueClient.orElseThrow(),
        nabProperties,
        serverProperties.getPort(),
        appMetadata,
        logLevelOverrideExtensionOptional.orElse(null));
  }

  @Bean
  JettyEventListener jettyEventListener(Optional<ConsulService> consulService) {
    return new JettyEventListener(consulService.orElse(null));
  }
}
