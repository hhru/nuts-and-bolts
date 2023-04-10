package ru.hh.nab.telemetry.jdbc.internal.extractor;

import io.opentelemetry.api.common.AttributeKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import static io.opentelemetry.instrumentation.api.internal.AttributesExtractorUtil.internalSet;
import javax.annotation.Nullable;
import ru.hh.nab.telemetry.jdbc.internal.model.JdbcOperationKind;

public class NabJdbcOperationKindExtractor<REQUEST> implements AttributesExtractor<REQUEST, Void> {

  private static final AttributeKey<String> JDBC_OPERATION_KIND_ATTRIBUTE_KEY = stringKey("jdbc.operation.kind");

  private final JdbcOperationKind operationKind;

  public NabJdbcOperationKindExtractor(JdbcOperationKind operationKind) {
    this.operationKind = operationKind;
  }

  @Override
  public void onStart(AttributesBuilder attributes, Context parentContext, REQUEST request) {
    internalSet(attributes, JDBC_OPERATION_KIND_ATTRIBUTE_KEY, operationKind.name());
  }

  @Override
  public void onEnd(AttributesBuilder attributes, Context context, REQUEST request, @Nullable Void unused, @Nullable Throwable error) {}
}
