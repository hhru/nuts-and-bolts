package ru.hh.nab.jclient;

import jakarta.annotation.Nullable;
import jakarta.inject.Named;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import static java.util.concurrent.TimeUnit.MINUTES;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.jclient.common.HttpClientContext;
import ru.hh.jclient.common.HttpClientContextThreadLocalSupplier;
import ru.hh.jclient.common.HttpClientEventListener;
import ru.hh.jclient.common.HttpClientFactoryBuilder;
import ru.hh.jclient.common.check.GlobalTimeoutCheck;
import ru.hh.jclient.common.util.storage.MDCStorage;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import static ru.hh.nab.jclient.UriCompactionUtil.compactUri;
import ru.hh.nab.jclient.checks.TransactionalCheck;

@Configuration
public class NabJClientConfig {
  @Bean
  HttpClientFactoryBuilder httpClientFactoryBuilder(
      @Named(SERVICE_NAME) String serviceName,
      HttpClientContextThreadLocalSupplier contextSupplier,
      ScheduledExecutorService scheduledExecutorService,
      List<HttpClientEventListener> eventListeners,
      FileSettings fileSettings
  ) {
    var subSettings = fileSettings.getSubSettings("jclient.listener.timeout-check");

    long thresholdMs = ofNullable(subSettings.getLong("threshold.ms")).orElse(100L);
    int minCompactionLength = ofNullable(subSettings.getInteger("min.compaction.length")).orElse(4);
    int minHashLength = ofNullable(subSettings.getInteger("min.hash.length")).orElse(16);
    long sendIntervalMinutes = ofNullable(subSettings.getInteger("send.interval.minutes")).orElse(1);
    return new HttpClientFactoryBuilder(contextSupplier, eventListeners)
        .addEventListener(
            new GlobalTimeoutCheck(
                Duration.ofMillis(thresholdMs),
                scheduledExecutorService,
                uri -> compactUri(uri, minCompactionLength, minHashLength),
                MINUTES.toMillis(sendIntervalMinutes)
            )
        ).withUserAgent(serviceName);
  }

  @Bean
  TransactionalCheck transactionalCheck(ScheduledExecutorService executorService, FileSettings fileSettings) {
    var subSettings = fileSettings.getSubSettings("jclient.listener.transactional-check");
    long sendIntervalMinutes = ofNullable(subSettings.getInteger("send.interval.minutes")).orElse(1);
    boolean failOnCheck = ofNullable(subSettings.getBoolean("fail.on.check")).orElse(Boolean.FALSE);
    int stacktraceDepthLimit = ofNullable(subSettings.getInteger("stacktrace.depth.limit")).orElse(10);
    var packagesToSkip = ofNullable(subSettings.getStringList("packages.to.skip")).map(Set::copyOf).orElseGet(Set::of);
    return new TransactionalCheck(
        failOnCheck ? TransactionalCheck.Action.RAISE : TransactionalCheck.Action.LOG,
        stacktraceDepthLimit,
        executorService, MINUTES.toMillis(sendIntervalMinutes),
        packagesToSkip
    );
  }

  @Bean
  HttpClientContextThreadLocalSupplier httpClientContextStorage(@Nullable HttpClientContext defaultContext) {
    return Optional.ofNullable(defaultContext)
        .map(ctx -> new HttpClientContextThreadLocalSupplier(() -> ctx))
        .orElseGet(HttpClientContextThreadLocalSupplier::new)
        .register(new MDCStorage());
  }

  @Bean
  JClientContextProviderFilter jClientContextProviderFilter(HttpClientContextThreadLocalSupplier contextSupplier) {
    return new JClientContextProviderFilter(contextSupplier);
  }
}
