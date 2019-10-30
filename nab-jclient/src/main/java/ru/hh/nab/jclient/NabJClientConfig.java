package ru.hh.nab.jclient;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
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
import static java.util.concurrent.TimeUnit.MINUTES;
import static ru.hh.nab.jclient.UriCompactionUtil.compactUrl;

@Configuration
public class NabJClientConfig {
  @Bean
  HttpClientFactoryBuilder httpClientFactoryBuilder(String serviceName, HttpClientContextThreadLocalSupplier contextSupplier,
                                                    ScheduledExecutorService scheduledExecutorService,
                                                    List<HttpClientEventListener> eventListeners) {
    return new HttpClientFactoryBuilder(contextSupplier, eventListeners)
      .addEventListener(
        new GlobalTimeoutCheck(Duration.ofMillis(100), scheduledExecutorService, uri -> compactUrl(uri, 4, 16), MINUTES.toMillis(1))
      ).withUserAgent(serviceName);
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
