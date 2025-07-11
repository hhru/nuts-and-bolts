package ru.hh.nab.jclient;

import jakarta.annotation.Nullable;
import jakarta.inject.Named;
import java.util.function.Supplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.jclient.common.HttpClientContext;
import ru.hh.jclient.common.HttpClientContextThreadLocalSupplier;
import ru.hh.jclient.common.HttpClientFactoryBuilder;
import ru.hh.jclient.common.util.storage.MDCStorage;
import static ru.hh.nab.common.qualifier.NamedQualifier.DEFAULT_HTTP_CLIENT_CONTEXT_SUPPLIER;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;

@Configuration
public class NabJClientConfig {

  @Bean
  HttpClientFactoryBuilder httpClientFactoryBuilder(
      @Named(SERVICE_NAME) String serviceName,
      HttpClientContextThreadLocalSupplier contextSupplier
  ) {
    return new HttpClientFactoryBuilder(contextSupplier).withUserAgent(serviceName);
  }

  @Bean
  HttpClientContextThreadLocalSupplier httpClientContextStorage(
      @Nullable HttpClientContext defaultContext,
      @Nullable @Named(DEFAULT_HTTP_CLIENT_CONTEXT_SUPPLIER) Supplier<HttpClientContext> defaultContextSupplier
  ) {
    HttpClientContextThreadLocalSupplier contextSupplier;

    if (defaultContextSupplier != null) {
      contextSupplier = new HttpClientContextThreadLocalSupplier(defaultContextSupplier, false);
    } else if (defaultContext != null) {
      contextSupplier = new HttpClientContextThreadLocalSupplier(() -> defaultContext);
    } else {
      contextSupplier = new HttpClientContextThreadLocalSupplier();
    }

    return contextSupplier.register(new MDCStorage());
  }

  @Bean
  JClientContextProviderFilter jClientContextProviderFilter(HttpClientContextThreadLocalSupplier contextSupplier) {
    return new JClientContextProviderFilter(contextSupplier);
  }
}
