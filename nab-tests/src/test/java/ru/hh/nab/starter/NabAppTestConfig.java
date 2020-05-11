package ru.hh.nab.starter;

import static org.mockito.Mockito.spy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.starter.events.JettyEventListener;
import ru.hh.nab.testbase.NabTestConfig;

@Configuration
@Import({NabTestConfig.class})
public class NabAppTestConfig {
  @Bean
  ConsulService consulService(FileSettings fileSettings, AppMetadata appMetadata) {
    return spy(new ConsulService(fileSettings, null, null, appMetadata));
  }

  @Bean
  JettyEventListener jettyEventConsulListener(ConsulService consulService) {
    return new JettyEventListener(consulService);
  }
}
