package ru.hh.nab.jclient;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.jclient.common.HttpClientContext;
import ru.hh.jclient.common.HttpClientContextThreadLocalSupplier;
import ru.hh.jclient.common.HttpClientEventListener;
import ru.hh.jclient.common.HttpClientFactoryBuilder;
import ru.hh.jclient.common.check.GlobalTimeoutCheck;
import ru.hh.jclient.common.util.storage.MDCStorage;
import ru.hh.nab.jclient.checks.TransactionalCheck;

import java.util.List;

@Configuration
public class NabJClientConfig {
  @Bean
  HttpClientFactoryBuilder httpClientFactoryBuilder(String serviceName, HttpClientContextThreadLocalSupplier contextSupplier,
                                                    List<HttpClientEventListener> eventListeners) {
    return new HttpClientFactoryBuilder(contextSupplier, eventListeners)
      .addEventListener(new GlobalTimeoutCheck())
      .withUserAgent(serviceName);
  }

  @Bean
  TransactionalCheck transactionalCheck() {
    return new TransactionalCheck();
  }

  @Bean
  HttpClientContextThreadLocalSupplier httpClientContextStorage(Optional<HttpClientContext> defaultContext) {
    return defaultContext.map(ctx -> new HttpClientContextThreadLocalSupplier(() -> ctx))
      .orElseGet(HttpClientContextThreadLocalSupplier::new)
      .register(new MDCStorage());
  }

  @Bean
  JClientContextProviderFilter jClientContextProviderFilter(HttpClientContextThreadLocalSupplier contextSupplier) {
    return new JClientContextProviderFilter(contextSupplier);
  }
}
