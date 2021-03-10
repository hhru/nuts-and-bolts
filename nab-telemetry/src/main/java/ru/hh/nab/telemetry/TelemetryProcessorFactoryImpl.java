package ru.hh.nab.telemetry;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.TextMapPropagator;
import ru.hh.jclient.common.RequestDebug;

public class TelemetryProcessorFactoryImpl implements TelemetryProcessorFactory {
  private final Tracer tracer;
  private final TextMapPropagator textMapPropagator;

  public TelemetryProcessorFactoryImpl(Tracer tracer, TextMapPropagator textMapPropagator) {
    this.tracer = tracer;
    this.textMapPropagator = textMapPropagator;
  }

  @Override
  public RequestDebug createRequestDebug() {
    return new TelemetryListenerImpl(tracer, textMapPropagator);
  }
}
