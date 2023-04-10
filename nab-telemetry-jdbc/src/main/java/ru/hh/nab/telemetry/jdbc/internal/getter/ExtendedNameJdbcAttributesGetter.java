package ru.hh.nab.telemetry.jdbc.internal.getter;

import javax.annotation.Nullable;
import ru.hh.nab.telemetry.jdbc.internal.extractor.DataSourceNameExtractor;
import ru.hh.nab.telemetry.jdbc.internal.model.NabDbRequest;

public class ExtendedNameJdbcAttributesGetter extends NabJdbcAttributesGetter {

  private static final DataSourceNameExtractor dataSourceNameExtractor = new DataSourceNameExtractor();

  @Nullable
  @Override
  public String getName(NabDbRequest request) {
    return dataSourceNameExtractor.extract(request.getNabDataSourceInfo()) + " " + super.getName(request);
  }
}
