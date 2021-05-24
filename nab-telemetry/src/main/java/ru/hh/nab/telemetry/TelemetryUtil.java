package ru.hh.nab.telemetry;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import java.util.Map;

public class TelemetryUtil {

  public static void setAttributes(Span span, Map<String, ?> attributes) {
    AttributesBuilder attributesBuilder = Attributes.builder();
    attributes.forEach((key, value) -> attributesBuilder.put(key, String.valueOf(value)));
    span.addEvent("ATTR", attributesBuilder.build());
  }
}
