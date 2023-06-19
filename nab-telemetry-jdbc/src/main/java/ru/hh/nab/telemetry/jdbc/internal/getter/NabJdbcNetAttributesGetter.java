package ru.hh.nab.telemetry.jdbc.internal.getter;

import io.opentelemetry.instrumentation.api.instrumenter.net.NetClientAttributesGetter;
import io.opentelemetry.instrumentation.jdbc.internal.JdbcNetAttributesGetter;
import jakarta.annotation.Nullable;
import ru.hh.nab.telemetry.jdbc.internal.model.NabDbRequest;

public class NabJdbcNetAttributesGetter implements NetClientAttributesGetter<NabDbRequest, Void> {

  private static final JdbcNetAttributesGetter jdbcNetAttributesGetter = new JdbcNetAttributesGetter();

  @Nullable
  @Override
  public String getTransport(NabDbRequest request, @Nullable Void unused) {
    return jdbcNetAttributesGetter.getTransport(request.getDbRequest(), unused);
  }

  @Nullable
  @Override
  public String getPeerName(NabDbRequest request) {
    return jdbcNetAttributesGetter.getPeerName(request.getDbRequest());
  }

  @Nullable
  @Override
  public Integer getPeerPort(NabDbRequest request) {
    return jdbcNetAttributesGetter.getPeerPort(request.getDbRequest());
  }
}
