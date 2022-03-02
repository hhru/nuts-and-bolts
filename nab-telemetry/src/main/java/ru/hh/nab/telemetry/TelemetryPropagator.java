package ru.hh.nab.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.Map;
import ru.hh.jclient.common.Request;

public class TelemetryPropagator {
  private final TextMapPropagator textMapPropagator;

  private static final TextMapGetter<Map<String, String>> GETTER = createGetter();
  private static final TextMapSetter<Request> SETTER = createSetter();

  public TelemetryPropagator(OpenTelemetry openTelemetry) {
    this.textMapPropagator = openTelemetry.getPropagators().getTextMapPropagator();
  }

  public Context getTelemetryContext(Context context, Map<String, String> requestHeadersMap) {
    return textMapPropagator.extract(context, requestHeadersMap, GETTER);
  }

  public void propagate(Request request) {
    textMapPropagator.inject(Context.current(), request, SETTER);
  }

  private static TextMapSetter<Request> createSetter() {
    return (carrier, key, value) -> carrier.getHeaders().set(key, value);
  }

  private static TextMapGetter<Map<String, String>> createGetter() {
    return new TextMapGetter<>() {
      @Override
      public Iterable<String> keys(Map<String, String> carrier) {
        return carrier.keySet();
      }

      @Override
      public String get(Map<String, String> carrier, String key) {
        return carrier.get(key);
      }
    };
  }

  public static StatusCode getStatus(int httpStatusCode, boolean isServer) {
    if (httpStatusCode < 100) {
      return StatusCode.ERROR;
    } else if (httpStatusCode <= 399) {
      return StatusCode.UNSET;
    } else if (httpStatusCode <= 499 && isServer) {
      return StatusCode.UNSET;
    }

    return StatusCode.ERROR;
  }
}
