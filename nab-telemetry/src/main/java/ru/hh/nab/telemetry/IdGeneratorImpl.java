package ru.hh.nab.telemetry;

import io.opentelemetry.sdk.trace.IdGenerator;
import ru.hh.trace.TraceContextUnsafe;
import ru.hh.trace.TraceIdGenerator;

public class IdGeneratorImpl implements IdGenerator {

  private final TraceContextUnsafe traceContext;

  public IdGeneratorImpl(TraceContextUnsafe traceContext) {
    this.traceContext = traceContext;
  }

  @Override
  public String generateSpanId() {
    return IdGenerator.random().generateSpanId();
  }

  @Override
  public String generateTraceId() {
    return traceContext.getStrictTraceId().orElseGet(TraceIdGenerator::generateTraceId);
  }
}
