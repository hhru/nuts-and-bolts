package ru.hh.nab.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.consul.AgentClient;
import ru.hh.consul.KeyValueClient;
import ru.hh.consul.config.ClientConfig;
import ru.hh.consul.monitoring.ClientEventCallback;
import ru.hh.consul.monitoring.ClientEventHandler;
import ru.hh.nab.testbase.NabTestConfig;
import ru.hh.nab.web.consul.ConsulProperties;
import ru.hh.nab.web.consul.ConsulService;
import ru.hh.nab.web.starter.configuration.properties.InfrastructureProperties;
import ru.hh.nab.web.starter.discovery.ServiceDiscoveryInitializer;

@Configuration
@Import({NabTestConfig.class})
public class NabAppTestConfig {

  @Bean
  @ConfigurationProperties(ConsulProperties.PREFIX)
  ConsulProperties consulProperties() {
    return new ConsulProperties();
  }

  @Bean
  ConsulService consulService(
      InfrastructureProperties infrastructureProperties,
      BuildProperties buildProperties,
      ServerProperties serverProperties,
      ConsulProperties consulProperties,
      AgentClient agentClient,
      KeyValueClient keyValueClient
  ) {
    ConsulService consulService = new ConsulService(
        agentClient,
        keyValueClient,
        infrastructureProperties.getServiceName(),
        buildProperties.getVersion(),
        infrastructureProperties.getNodeName(),
        serverProperties.getPort(),
        consulProperties,
        null
    );
    return spy(consulService);
  }

  @Bean
  AgentClient agentClient() {
    return mock(AgentClient.class);
  }

  @Bean
  KeyValueClient keyValueClient() {
    KeyValueClient mock = mock(KeyValueClient.class);
    when(mock.getConfig()).thenReturn(new ClientConfig());
    when(mock.getEventHandler()).thenReturn(new ClientEventHandler("test", new ClientEventCallback() {}));
    return mock;
  }

  @Bean
  ServiceDiscoveryInitializer serviceRegistrator(ConsulService consulService) {
    return new ServiceDiscoveryInitializer(consulService);
  }
}
