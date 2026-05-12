package ru.hh.nab.telemetry;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.EXCEPTION_EVENT_NAME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.EXCEPTION_MESSAGE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.EXCEPTION_TYPE;
import static java.util.Optional.ofNullable;

public final class TelemetryUtils {

  public static void addExceptionEventToSpan(Span span, Throwable throwable) {
    span.addEvent(
        EXCEPTION_EVENT_NAME,
        Attributes.of(
            EXCEPTION_TYPE, throwable.getClass().getName(),
            EXCEPTION_MESSAGE, ofNullable(throwable.getMessage()).orElse("")
        )
    );
  }

  private TelemetryUtils() {
  }
}
