package ru.hh.nab.telemetry.jdbc.internal.extractor;

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import static io.opentelemetry.instrumentation.api.internal.AttributesExtractorUtil.internalSet;
import java.util.function.Function;
import javax.annotation.Nullable;
import ru.hh.nab.telemetry.jdbc.internal.model.NabDataSourceInfo;

public class NabDataSourceInfoExtractor<REQUEST> implements AttributesExtractor<REQUEST, Void> {

  private final Function<REQUEST, NabDataSourceInfo> nabDataSourceInfoGetter;

  public NabDataSourceInfoExtractor(Function<REQUEST, NabDataSourceInfo> nabDataSourceInfoGetter) {
    this.nabDataSourceInfoGetter = nabDataSourceInfoGetter;
  }

  @Override
  public void onStart(AttributesBuilder attributes, Context parentContext, REQUEST request) {
    nabDataSourceInfoGetter
        .apply(request)
        .getDataSourceName()
        .ifPresent(dataSourceName -> internalSet(attributes, NabDataSourceInfo.DATASOURCE_NAME_ATTRIBUTE_KEY, dataSourceName));
    nabDataSourceInfoGetter
        .apply(request)
        .isWritableDataSource()
        .ifPresent(writable -> internalSet(attributes, NabDataSourceInfo.DATASOURCE_WRITABLE_ATTRIBUTE_KEY, writable));
  }

  @Override
  public void onEnd(AttributesBuilder attributes, Context context, REQUEST request, @Nullable Void response, @Nullable Throwable error) {}
}
