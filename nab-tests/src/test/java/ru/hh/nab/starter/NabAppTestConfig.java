package ru.hh.nab.starter;

import jakarta.inject.Named;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.consul.AgentClient;
import ru.hh.consul.KeyValueClient;
import ru.hh.consul.config.ClientConfig;
import ru.hh.consul.monitoring.ClientEventCallback;
import ru.hh.consul.monitoring.ClientEventHandler;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.qualifier.NamedQualifier.NODE_NAME;
import ru.hh.nab.starter.consul.ConsulService;
import ru.hh.nab.testbase.NabTestConfig;
import ru.hh.nab.web.JettyEventListener;

@Configuration
@Import({NabTestConfig.class})
public class NabAppTestConfig {

  @Bean
  ConsulService consulService(FileSettings fileSettings, AppMetadata appMetadata, @Named(NODE_NAME) String nodeName,
                              AgentClient agentClient, KeyValueClient keyValueClient) {
    return spy(new ConsulService(agentClient, keyValueClient, fileSettings, appMetadata, nodeName, null));
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
  JettyEventListener jettyEventConsulListener(ConsulService consulService) {
    return new JettyEventListener(consulService);
  }
}
