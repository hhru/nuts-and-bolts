package ru.hh.nab.telemetry.jdbc.internal.getter;

import io.opentelemetry.instrumentation.api.incubator.semconv.db.SqlClientAttributesGetter;
import io.opentelemetry.instrumentation.jdbc.internal.JdbcAttributesGetter;
import jakarta.annotation.Nullable;
import java.util.Collection;
import ru.hh.nab.telemetry.jdbc.internal.model.NabDbRequest;

public class NabJdbcAttributesGetter implements SqlClientAttributesGetter<NabDbRequest, Void> {

  private static final JdbcAttributesGetter dbAttributesGetter = new JdbcAttributesGetter();

  @Nullable
  @Override
  public String getSystem(NabDbRequest request) {
    return dbAttributesGetter.getDbSystem(request.getDbRequest());
  }

  @Nullable
  @Override
  public String getUser(NabDbRequest request) {
    return dbAttributesGetter.getUser(request.getDbRequest());
  }

  @Nullable
  @Override
  public String getName(NabDbRequest request) {
    return dbAttributesGetter.getDbNamespace(request.getDbRequest());
  }

  @Nullable
  @Override
  public String getConnectionString(NabDbRequest request) {
    return dbAttributesGetter.getConnectionString(request.getDbRequest());
  }

  @Nullable
  @Override
  public String getRawStatement(NabDbRequest request) {
    Collection<String> texts = dbAttributesGetter.getRawQueryTexts(request.getDbRequest());
    return texts.isEmpty() ? null : texts.iterator().next();
  }
}
