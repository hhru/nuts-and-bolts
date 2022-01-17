package ru.hh.nab.telemetry;

import io.opentelemetry.api.trace.Tracer;
import ru.hh.jclient.common.RequestDebug;

public class TelemetryProcessorFactory {
  private final Tracer tracer;
  private final TelemetryPropagator telemetryPropagator;

  public TelemetryProcessorFactory(Tracer tracer, TelemetryPropagator telemetryPropagator) {
    this.tracer = tracer;
    this.telemetryPropagator = telemetryPropagator;
  }

  public RequestDebug createRequestDebug() {
    return new TelemetryListenerImpl(tracer, telemetryPropagator);
  }
}
