package ru.hh.nab.telemetry;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.function.Function;
import ru.hh.jclient.common.RequestDebug;
import ru.hh.jclient.common.Uri;

public class TelemetryProcessorFactory {
  private final Tracer tracer;
  private final TextMapPropagator textMapPropagator;
  private final Function<Uri, String> uriCompactionFunction;

  public TelemetryProcessorFactory(Tracer tracer, TextMapPropagator textMapPropagator, Function<Uri, String> uriCompactionFunction) {
    this.tracer = tracer;
    this.textMapPropagator = textMapPropagator;
    this.uriCompactionFunction = uriCompactionFunction;
  }

  public RequestDebug createRequestDebug() {
    return new TelemetryListenerImpl(tracer, textMapPropagator, uriCompactionFunction);
  }
}
