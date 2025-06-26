package ru.hh.nab.telemetry;

import io.opentelemetry.api.trace.Tracer;
import ru.hh.jclient.common.HttpClientEventListener;
import ru.hh.trace.TraceContextUnsafe;

public class TelemetryProcessorFactory {
  private final Tracer tracer;
  private final TelemetryPropagator telemetryPropagator;
  private final TraceContextUnsafe traceContext;

  public TelemetryProcessorFactory(Tracer tracer, TelemetryPropagator telemetryPropagator, TraceContextUnsafe traceContext) {
    this.tracer = tracer;
    this.telemetryPropagator = telemetryPropagator;
    this.traceContext = traceContext;
  }

  public HttpClientEventListener createHttpClientEventListener() {
    return new TelemetryListenerImpl(tracer, telemetryPropagator, traceContext);
  }
}
