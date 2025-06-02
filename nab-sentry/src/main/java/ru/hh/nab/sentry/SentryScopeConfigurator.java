package ru.hh.nab.sentry;

import io.sentry.Sentry;
import io.sentry.protocol.SentryId;
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
        // TODO: it's better to use warn/error log level, but there are too much invalid rids.
        //  Fix log level to warn/error after https://jira.hh.ru/browse/PORTFOLIO-19764
        LOGGER.debug("Unable to set sentry trace id: {}", traceId, e);
      }
    });
  }

  public static void clearTraceId() {
    Sentry.configureScope(scope -> scope.getPropagationContext().setTraceId(SentryId.EMPTY_ID));
  }

  private SentryScopeConfigurator() {
  }
}
