package ru.hh.nab.telemetry.jdbc.internal.getter;

import io.opentelemetry.instrumentation.api.incubator.semconv.code.CodeAttributesGetter;
import ru.hh.nab.telemetry.jdbc.internal.model.NabDataSourceInfo;

public class ConnectionCodeAttributesGetter implements CodeAttributesGetter<NabDataSourceInfo> {

  @Override
  public Class<?> getCodeClass(NabDataSourceInfo nabDataSourceInfo) {
    return nabDataSourceInfo.getDataSource().getClass();
  }

  @Override
  public String getMethodName(NabDataSourceInfo nabDataSourceInfo) {
    return "getConnection";
  }
}
