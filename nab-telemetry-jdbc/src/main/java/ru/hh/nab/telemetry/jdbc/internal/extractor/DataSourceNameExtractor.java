package ru.hh.nab.telemetry.jdbc.internal.extractor;

import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.code.CodeAttributesGetter;
import io.opentelemetry.instrumentation.api.instrumenter.code.CodeSpanNameExtractor;
import javax.sql.DataSource;
import ru.hh.nab.telemetry.jdbc.internal.model.NabDataSourceInfo;

public class DataSourceNameExtractor implements SpanNameExtractor<NabDataSourceInfo> {

  private final static SpanNameExtractor<DataSource> DATA_SOURCE_SPAN_NAME_EXTRACTOR =
      CodeSpanNameExtractor.create(new DataSourceCodeAttributesGetter());

  @Override
  public String extract(NabDataSourceInfo nabDataSourceInfo) {
    return nabDataSourceInfo
        .getDataSourceName()
        .orElseGet(() -> DATA_SOURCE_SPAN_NAME_EXTRACTOR.extract(nabDataSourceInfo.getDataSource()));
  }

  private static class DataSourceCodeAttributesGetter implements CodeAttributesGetter<DataSource> {

    @Override
    public Class<?> getCodeClass(DataSource dataSource) {
      return dataSource.getClass();
    }

    @Override
    public String getMethodName(DataSource dataSource) {
      // force to return only class via CodeSpanNameExtractor without method
      return null;
    }
  }
}
