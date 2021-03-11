package ru.hh.nab.telemetry;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.TextMapPropagator;
import ru.hh.jclient.common.RequestDebug;

public class TelemetryProcessorFactory {
  private final Tracer tracer;
  private final TextMapPropagator textMapPropagator;

  public TelemetryProcessorFactory(Tracer tracer, TextMapPropagator textMapPropagator) {
    this.tracer = tracer;
    this.textMapPropagator = textMapPropagator;
  }

  public RequestDebug createRequestDebug() {
    return new TelemetryListenerImpl(tracer, textMapPropagator);
  }
}
