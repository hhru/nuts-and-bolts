package ru.hh.nab.starter;

import com.orbitz.consul.AgentClient;
import com.orbitz.consul.KeyValueClient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.starter.events.JettyEventListener;
import ru.hh.nab.testbase.NabTestConfig;

import java.util.Optional;

@Configuration
@Import({NabTestConfig.class})
public class NabAppTestConfig {

  @Bean
  ConsulService consulService(FileSettings fileSettings, AppMetadata appMetadata, AgentClient agentClient, KeyValueClient keyValueClient) {
    return spy(new ConsulService(agentClient, keyValueClient, fileSettings, "localhost", appMetadata, null));
  }

  @Bean
  AgentClient agentClient() {
    return mock(AgentClient.class);
  }

  @Bean
  KeyValueClient keyValueClient() {
    KeyValueClient mock = mock(KeyValueClient.class);
    when(mock.getValueAsString("host/localhost/weight")).thenReturn(Optional.of("204"));
    return mock;
  }

  @Bean
  JettyEventListener jettyEventConsulListener(ConsulService consulService) {
    return new JettyEventListener(consulService);
  }
}
