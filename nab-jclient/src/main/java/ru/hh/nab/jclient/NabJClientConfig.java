package ru.hh.nab.jclient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.jclient.common.HttpClientEventListener;
import ru.hh.jclient.common.HttpClientFactoryBuilder;
import ru.hh.nab.jclient.checks.TransactionalCheck;

import java.util.List;

@Configuration
public class NabJClientConfig {
  @Bean
  public HttpClientFactoryBuilder httpClientFactoryBuilder(String serviceName, List<HttpClientEventListener> eventListeners) {
    return new HttpClientFactoryBuilder()
      .withEventListeners(eventListeners)
      .withUserAgent(serviceName);
  }

  @Bean
  public TransactionalCheck transactionalCheck() {
    return new TransactionalCheck();
  }
}
