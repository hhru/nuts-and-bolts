package ru.hh.nab.jclient;

import java.time.Duration;
import static java.util.Optional.ofNullable;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import static java.util.concurrent.TimeUnit.MINUTES;
import org.springframework.context.annotation.Configuration;
import ru.hh.jclient.common.HttpClientContextThreadLocalSupplier;
import ru.hh.jclient.common.listener.GlobalTimeoutCheck;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.jclient.UriCompactionUtil.compactUri;
import ru.hh.nab.jclient.checks.TransactionalCheck;

@Configuration
public class EventListenerConfig {
  public EventListenerConfig(
      ScheduledExecutorService scheduledExecutorService,
      HttpClientContextThreadLocalSupplier contextSupplier,
      FileSettings fileSettings
  ) {
    registerGlobalTimeoutCheck(scheduledExecutorService, contextSupplier, fileSettings);
    registerTransactionalCheck(scheduledExecutorService, contextSupplier, fileSettings);
  }

  void registerGlobalTimeoutCheck(
      ScheduledExecutorService scheduledExecutorService,
      HttpClientContextThreadLocalSupplier contextSupplier,
      FileSettings fileSettings
  ) {
    var subSettings = fileSettings.getSubSettings("jclient.listener.timeout-check");

    long thresholdMs = ofNullable(subSettings.getLong("threshold.ms")).orElse(100L);
    int minCompactionLength = ofNullable(subSettings.getInteger("min.compaction.length")).orElse(4);
    int minHashLength = ofNullable(subSettings.getInteger("min.hash.length")).orElse(16);
    long sendIntervalMinutes = ofNullable(subSettings.getInteger("send.interval.minutes")).orElse(1);

    var checker = new GlobalTimeoutCheck(
        Duration.ofMillis(thresholdMs),
        scheduledExecutorService,
        uri -> compactUri(uri, minCompactionLength, minHashLength),
        MINUTES.toMillis(sendIntervalMinutes)
    );
    contextSupplier.registerEventListenerSupplier(() -> checker);
  }

  void registerTransactionalCheck(
      ScheduledExecutorService executorService,
      HttpClientContextThreadLocalSupplier contextSupplier,
      FileSettings fileSettings
  ) {
    var subSettings = fileSettings.getSubSettings("jclient.listener.transactional-check");
    long sendIntervalMinutes = ofNullable(subSettings.getInteger("send.interval.minutes")).orElse(1);
    boolean failOnCheck = ofNullable(subSettings.getBoolean("fail.on.check")).orElse(Boolean.FALSE);
    int stacktraceDepthLimit = ofNullable(subSettings.getInteger("stacktrace.depth.limit")).orElse(10);
    var packagesToSkip = ofNullable(subSettings.getStringList("packages.to.skip")).map(Set::copyOf).orElseGet(Set::of);
    var checker = new TransactionalCheck(
        failOnCheck ? TransactionalCheck.Action.RAISE : TransactionalCheck.Action.LOG,
        stacktraceDepthLimit,
        executorService, MINUTES.toMillis(sendIntervalMinutes),
        packagesToSkip
    );
    contextSupplier.registerEventListenerSupplier(() -> checker);
  }
}
