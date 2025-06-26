package ru.hh.nab.common.mdc;

import static java.util.Optional.ofNullable;
import ru.hh.trace.Scope;
import ru.hh.trace.TraceContextListener;

public class MDCTraceContextListener implements TraceContextListener {

  @Override
  public Scope onTraceStart(String traceId, String strictTraceId) {
    String previousTraceId = MDC.getRequestId().orElse(null);
    MDC.setRequestId(traceId);
    return new MDCTraceContextListenerScope(previousTraceId);
  }

  private record MDCTraceContextListenerScope(String previousTraceId) implements Scope {
    @Override
    public void close() {
      ofNullable(previousTraceId).ifPresentOrElse(MDC::setRequestId, MDC::clearRequestId);
    }
  }
}
