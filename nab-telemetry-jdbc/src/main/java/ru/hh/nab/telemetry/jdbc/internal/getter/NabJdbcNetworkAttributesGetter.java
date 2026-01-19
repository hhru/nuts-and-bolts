package ru.hh.nab.telemetry.jdbc.internal.getter;

import io.opentelemetry.instrumentation.api.semconv.network.ServerAttributesGetter;
import io.opentelemetry.instrumentation.jdbc.internal.JdbcNetworkAttributesGetter;
import jakarta.annotation.Nullable;
import ru.hh.nab.telemetry.jdbc.internal.model.NabDbRequest;

public class NabJdbcNetworkAttributesGetter implements ServerAttributesGetter<NabDbRequest> {

  private static final JdbcNetworkAttributesGetter jdbcNetAttributesGetter = new JdbcNetworkAttributesGetter();

  @Nullable
  @Override
  public String getServerAddress(NabDbRequest request) {
    return jdbcNetAttributesGetter.getServerAddress(request.getDbRequest());
  }

  @Nullable
  @Override
  public Integer getServerPort(NabDbRequest request) {
    return jdbcNetAttributesGetter.getServerPort(request.getDbRequest());
  }
}
