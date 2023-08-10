package ru.hh.nab.jclient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import static java.util.Optional.ofNullable;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import static java.util.concurrent.TimeUnit.MINUTES;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.qualifier.NamedQualifier.COMMON_SCHEDULED_EXECUTOR;
import ru.hh.nab.jclient.checks.TransactionalCheck;

@ApplicationScoped
public class TransactionalCheckConfig {
  @Produces
  @Singleton
  TransactionalCheck transactionalCheck(@Named(COMMON_SCHEDULED_EXECUTOR) ScheduledExecutorService executorService, FileSettings fileSettings) {
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
}
