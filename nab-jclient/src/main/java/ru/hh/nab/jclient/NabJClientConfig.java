package ru.hh.nab.jclient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import java.util.concurrent.ScheduledExecutorService;
import static java.util.concurrent.TimeUnit.MINUTES;
import ru.hh.jclient.common.HttpClientContext;
import ru.hh.jclient.common.HttpClientContextThreadLocalSupplier;
import ru.hh.jclient.common.HttpClientEventListener;
import ru.hh.jclient.common.HttpClientFactoryBuilder;
import ru.hh.jclient.common.check.GlobalTimeoutCheck;
import ru.hh.jclient.common.util.storage.MDCStorage;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.qualifier.NamedQualifier.COMMON_SCHEDULED_EXECUTOR;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import static ru.hh.nab.jclient.UriCompactionUtil.compactUri;

@ApplicationScoped
public class NabJClientConfig {
  @Produces
  @Singleton
  HttpClientFactoryBuilder httpClientFactoryBuilder(
      @Named(SERVICE_NAME) String serviceName,
      HttpClientContextThreadLocalSupplier contextSupplier,
      @Named(COMMON_SCHEDULED_EXECUTOR) ScheduledExecutorService scheduledExecutorService,
      Instance<HttpClientEventListener> eventListeners,
      FileSettings fileSettings
  ) {
    var subSettings = fileSettings.getSubSettings("jclient.listener.timeout-check");

    long thresholdMs = ofNullable(subSettings.getLong("threshold.ms")).orElse(100L);
    int minCompactionLength = ofNullable(subSettings.getInteger("min.compaction.length")).orElse(4);
    int minHashLength = ofNullable(subSettings.getInteger("min.hash.length")).orElse(16);
    long sendIntervalMinutes = ofNullable(subSettings.getInteger("send.interval.minutes")).orElse(1);
    return new HttpClientFactoryBuilder(contextSupplier, eventListeners.stream().toList())
        .addEventListener(
            new GlobalTimeoutCheck(
                Duration.ofMillis(thresholdMs),
                scheduledExecutorService,
                uri -> compactUri(uri, minCompactionLength, minHashLength),
                MINUTES.toMillis(sendIntervalMinutes)
            )
        ).withUserAgent(serviceName);
  }

  @Produces
  @Singleton
  HttpClientContextThreadLocalSupplier httpClientContextStorage(Instance<HttpClientContext> defaultContext) {
    return Optional.ofNullable(defaultContext.isResolvable() ? defaultContext.get() : null)
        .map(ctx -> new HttpClientContextThreadLocalSupplier(() -> ctx))
        .orElseGet(HttpClientContextThreadLocalSupplier::new)
        .register(new MDCStorage());
  }

  @Produces
  @Singleton
  JClientContextProviderFilter jClientContextProviderFilter(HttpClientContextThreadLocalSupplier contextSupplier) {
    return new JClientContextProviderFilter(contextSupplier);
  }
}
