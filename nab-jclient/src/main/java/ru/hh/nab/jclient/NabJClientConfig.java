package ru.hh.nab.jclient;

import java.util.function.Supplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.jclient.common.HttpClientContextThreadLocalSupplier;
import ru.hh.jclient.common.HttpClientEventListener;
import ru.hh.jclient.common.HttpClientFactoryBuilder;
import ru.hh.jclient.common.RequestDebug;
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
  HttpClientContextThreadLocalSupplier httpClientContextStorage(Supplier<RequestDebug> requestDebugSupplier) {
    return new HttpClientContextThreadLocalSupplier(requestDebugSupplier)
      .register(new MDCStorage());
  }

  @Bean
  JClientContextProviderFilter jClientContextProviderFilter(HttpClientContextThreadLocalSupplier contextSupplier) {
    return new JClientContextProviderFilter(contextSupplier);
  }
}
