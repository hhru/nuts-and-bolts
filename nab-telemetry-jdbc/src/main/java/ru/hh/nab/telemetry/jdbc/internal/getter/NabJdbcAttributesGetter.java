package ru.hh.nab.telemetry.jdbc.internal.getter;

import io.opentelemetry.instrumentation.api.instrumenter.db.SqlClientAttributesGetter;
import io.opentelemetry.instrumentation.jdbc.internal.JdbcAttributesGetter;
import jakarta.annotation.Nullable;
import ru.hh.nab.telemetry.jdbc.internal.model.NabDbRequest;

public class NabJdbcAttributesGetter implements SqlClientAttributesGetter<NabDbRequest> {

  private static final JdbcAttributesGetter dbAttributesGetter = new JdbcAttributesGetter();

  @Nullable
  @Override
  public String getSystem(NabDbRequest request) {
    return dbAttributesGetter.getSystem(request.getDbRequest());
  }

  @Nullable
  @Override
  public String getUser(NabDbRequest request) {
    return dbAttributesGetter.getUser(request.getDbRequest());
  }

  @Nullable
  @Override
  public String getName(NabDbRequest request) {
    return dbAttributesGetter.getName(request.getDbRequest());
  }

  @Nullable
  @Override
  public String getConnectionString(NabDbRequest request) {
    return dbAttributesGetter.getConnectionString(request.getDbRequest());
  }

  @Nullable
  @Override
  public String getRawStatement(NabDbRequest request) {
    return dbAttributesGetter.getRawStatement(request.getDbRequest());
  }
}
