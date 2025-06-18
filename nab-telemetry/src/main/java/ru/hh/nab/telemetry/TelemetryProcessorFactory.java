package ru.hh.nab.telemetry;

import io.opentelemetry.api.trace.Tracer;
import ru.hh.jclient.common.HttpClientEventListener;

public class TelemetryProcessorFactory {
  private final Tracer tracer;
  private final TelemetryPropagator telemetryPropagator;

  public TelemetryProcessorFactory(Tracer tracer, TelemetryPropagator telemetryPropagator) {
    this.tracer = tracer;
    this.telemetryPropagator = telemetryPropagator;
  }

  public HttpClientEventListener createHttpClientEventListener() {
    return new TelemetryListenerImpl(tracer, telemetryPropagator);
  }
}
