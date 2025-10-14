package ru.hh.nab.sentry;

import static java.util.Optional.ofNullable;
import ru.hh.trace.Scope;
import ru.hh.trace.TraceContextListener;

public class SentryTraceContextListener implements TraceContextListener {

  @Override
  public Scope onTraceStart(String traceId, String strictTraceId) {
    String previousTraceId = SentryScopeConfigurator.getTraceId().orElse(null);
    SentryScopeConfigurator.setTraceId(strictTraceId);
    return new SentryTraceContextListenerScope(previousTraceId);
  }

  private record SentryTraceContextListenerScope(String previousTraceId) implements Scope {
    @Override
    public void close() {
      ofNullable(previousTraceId).ifPresentOrElse(SentryScopeConfigurator::setTraceId, SentryScopeConfigurator::clearTraceId);
    }
  }
}
