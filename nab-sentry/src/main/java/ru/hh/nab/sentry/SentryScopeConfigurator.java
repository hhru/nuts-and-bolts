package ru.hh.nab.sentry;

import io.sentry.Sentry;
import io.sentry.protocol.SentryId;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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

  /**
   * Reads the trace id straight from the propagation context, the same place {@link #setTraceId(String)} and
   * {@link #clearTraceId()} write to.
   *
   * <p>{@code Sentry.getTraceparent()} would return the very same id, but on its way there it assembles a full
   * {@code Baggage} header and URL-encodes it with {@link String#replaceAll(String, String)}, which recompiles a
   * regex {@code Pattern} on every single call. Since this method is invoked once per request, that showed up as
   * roughly 13% of all allocation in hh-xmlback-online.
   */
  @SuppressWarnings("UnstableApiUsage")
  public static Optional<String> getTraceId() {
    AtomicReference<SentryId> traceId = new AtomicReference<>();
    Sentry.configureScope(scope -> traceId.set(scope.getPropagationContext().getTraceId()));
    return Optional.ofNullable(traceId.get()).map(SentryId::toString);
  }

  @SuppressWarnings("UnstableApiUsage")
  public static void clearTraceId() {
    Sentry.configureScope(scope -> scope.getPropagationContext().setTraceId(SentryId.EMPTY_ID));
  }

  private SentryScopeConfigurator() {
  }
}
