package ru.hh.nab.sentry;

import io.sentry.Sentry;
import io.sentry.SentryTraceHeader;
import io.sentry.protocol.SentryId;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SentryScopeConfigurator {

  private static final Logger LOGGER = LoggerFactory.getLogger(SentryScopeConfigurator.class);

  @SuppressWarnings("UnstableApiUsage")
  public static void setTraceId(String traceId) {
    Sentry.configureScope(scope -> {
      try {
        scope.getPropagationContext().setTraceId(new SentryId(traceId));
      } catch (RuntimeException e) {
        LOGGER.warn("Unable to set sentry trace id: {}", traceId, e);
      }
    });
  }

  public static Optional<String> getTraceId() {
    return Optional.ofNullable(Sentry.getTraceparent()).map(SentryTraceHeader::getTraceId).map(SentryId::toString);
  }

  @SuppressWarnings("UnstableApiUsage")
  public static void clearTraceId() {
    Sentry.configureScope(scope -> scope.getPropagationContext().setTraceId(SentryId.EMPTY_ID));
  }

  private SentryScopeConfigurator() {
  }
}
