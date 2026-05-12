package ru.hh.nab.telemetry.jclient;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import ru.hh.jclient.common.Request;

public class TelemetryPropagator {

  private static final TextMapSetter<Request> SETTER = createSetter();

  private final TextMapPropagator textMapPropagator;

  public TelemetryPropagator(OpenTelemetry openTelemetry) {
    this.textMapPropagator = openTelemetry.getPropagators().getTextMapPropagator();
  }

  public void propagate(Request request) {
    textMapPropagator.inject(Context.current(), request, SETTER);
  }

  private static TextMapSetter<Request> createSetter() {
    return (carrier, key, value) -> carrier.getHeaders().set(key, value);
  }

  public static StatusCode getStatus(int httpStatusCode) {
    if (httpStatusCode < 100) {
      return StatusCode.ERROR;
    } else if (httpStatusCode <= 399) {
      return StatusCode.UNSET;
    }

    return StatusCode.ERROR;
  }
}
