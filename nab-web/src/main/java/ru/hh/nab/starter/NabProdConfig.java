package ru.hh.nab.starter;

import jakarta.annotation.Nullable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.starter.consul.ConsulService;
import ru.hh.nab.starter.events.JettyEventListener;

@Configuration
@Import({NabCommonConfig.class})
public class NabProdConfig {

  public static final String CONSUL_PORT_PROPERTY = "consul.http.port";
  public static final String CONSUL_HOST_PROPERTY = "consul.http.host";
  static final String PROPERTIES_FILE_NAME = "service.properties";

  @Bean
  JettyEventListener jettyEventListener(@Nullable ConsulService consulService) {
    return new JettyEventListener(consulService);
  }
}
