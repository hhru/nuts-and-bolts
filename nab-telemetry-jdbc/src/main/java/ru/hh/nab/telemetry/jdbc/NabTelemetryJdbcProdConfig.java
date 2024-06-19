package ru.hh.nab.telemetry.jdbc;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.code.CodeAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.db.DbClientSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.db.SqlClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.net.NetClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.internal.ConfigPropertiesUtil;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.jdbc.common.ext.OpenTelemetryJdbcExtension;
import ru.hh.nab.telemetry.jdbc.internal.extractor.ConnectionSpanNameExtractor;
import ru.hh.nab.telemetry.jdbc.internal.extractor.NabDataSourceInfoExtractor;
import ru.hh.nab.telemetry.jdbc.internal.extractor.NabJdbcOperationKindExtractor;
import ru.hh.nab.telemetry.jdbc.internal.getter.ConnectionCodeAttributesGetter;
import ru.hh.nab.telemetry.jdbc.internal.getter.ExtendedNameJdbcAttributesGetter;
import ru.hh.nab.telemetry.jdbc.internal.getter.NabJdbcAttributesGetter;
import ru.hh.nab.telemetry.jdbc.internal.getter.NabJdbcNetAttributesGetter;
import ru.hh.nab.telemetry.jdbc.internal.model.JdbcOperationKind;
import ru.hh.nab.telemetry.jdbc.internal.model.NabDataSourceInfo;
import ru.hh.nab.telemetry.jdbc.internal.model.NabDbRequest;
import ru.hh.nab.telemetry.jdbc.internal.qualifier.Connection;
import ru.hh.nab.telemetry.jdbc.internal.qualifier.Statement;

@Configuration
public class NabTelemetryJdbcProdConfig {

  private static final String INSTRUMENTATION_NAME = "ru.hh.nab.telemetry.jdbc";

  @Bean
  public OpenTelemetryJdbcExtension openTelemetryJdbcExtension(NabTelemetryDataSourceFactory nabTelemetryDataSourceFactory) {
    return new NabTelemetryJdbcExtension(nabTelemetryDataSourceFactory);
  }

  @Bean
  public NabTelemetryDataSourceFactory nabTelemetryDataSourceFactory(
      FileSettings fileSettings,
      @Connection Instrumenter<NabDataSourceInfo, Void> connectionInstrumenter,
      @Statement Instrumenter<NabDbRequest, Void> statementInstrumenter
  ) {
    return new NabTelemetryDataSourceFactory(fileSettings, connectionInstrumenter, statementInstrumenter);
  }

  @Bean
  @Connection
  public Instrumenter<NabDataSourceInfo, Void> connectionInstrumenter(OpenTelemetry openTelemetry) {
    return Instrumenter
        .<NabDataSourceInfo, Void>builder(
            openTelemetry,
            INSTRUMENTATION_NAME,
            new ConnectionSpanNameExtractor()
        )
        .addAttributesExtractor(CodeAttributesExtractor.create(new ConnectionCodeAttributesGetter()))
        .addAttributesExtractor(new NabDataSourceInfoExtractor<>(Function.identity()))
        .addAttributesExtractor(new NabJdbcOperationKindExtractor<>(JdbcOperationKind.CONNECTION))
        .buildInstrumenter();
  }

  @Bean
  @Statement
  public Instrumenter<NabDbRequest, Void> statementInstrumenter(OpenTelemetry openTelemetry) {
    return Instrumenter
        .<NabDbRequest, Void>builder(
            openTelemetry,
            INSTRUMENTATION_NAME,
            DbClientSpanNameExtractor.create(new ExtendedNameJdbcAttributesGetter())
        )
        .addAttributesExtractor(
            SqlClientAttributesExtractor
                .builder(new NabJdbcAttributesGetter())
                .setStatementSanitizationEnabled(ConfigPropertiesUtil.getBoolean("otel.instrumentation.common.db-statement-sanitizer.enabled", true))
                .build()
        )
        .addAttributesExtractor(NetClientAttributesExtractor.create(new NabJdbcNetAttributesGetter()))
        .addAttributesExtractor(new NabDataSourceInfoExtractor<>(NabDbRequest::getNabDataSourceInfo))
        .addAttributesExtractor(new NabJdbcOperationKindExtractor<>(JdbcOperationKind.STATEMENT))
        .buildInstrumenter(SpanKindExtractor.alwaysClient());
  }
}
