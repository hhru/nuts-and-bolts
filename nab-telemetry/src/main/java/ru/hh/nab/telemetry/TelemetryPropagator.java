package ru.hh.nab.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.List;
import java.util.Map;

public class TelemetryPropagator {
  private final TextMapPropagator textMapPropagator;

  private static final TextMapGetter<Map<String, List<String>>> GETTER = createGetter();

  public TelemetryPropagator(OpenTelemetry openTelemetry) {
    this.textMapPropagator = openTelemetry.getPropagators().getTextMapPropagator();
  }

  public Context getTelemetryContext(Context context, Map<String, List<String>> requestHeadersMap) {
    return textMapPropagator.extract(context, requestHeadersMap, GETTER);
  }

  private static TextMapGetter<Map<String, List<String>>> createGetter() {
    return new TextMapGetter<>() {
      @Override
      public Iterable<String> keys(Map<String, List<String>> carrier) {
        return carrier.keySet();
      }

      @Override
      public String get(Map<String, List<String>> carrier, String key) {
        List<String> header = carrier.get(key);
        if (header == null || header.isEmpty()) {
          return null;
        }
        return header.get(0);
      }
    };
  }
}
