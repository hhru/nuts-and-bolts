package ru.hh.nab.telemetry;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_MESSAGE;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_TYPE;
import static java.util.Optional.ofNullable;

public final class TelemetryUtils {

  private static final String EXCEPTION_EVENT_NAME = "exception";

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
