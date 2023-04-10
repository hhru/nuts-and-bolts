package ru.hh.nab.telemetry.jdbc;

import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import javax.sql.DataSource;
import ru.hh.nab.telemetry.jdbc.internal.NabTelemetryDataSource;
import ru.hh.nab.telemetry.jdbc.internal.model.NabDataSourceInfo;
import ru.hh.nab.telemetry.jdbc.internal.model.NabDbRequest;

public class NabTelemetryDataSourceFactory {

  private final Instrumenter<NabDataSourceInfo, Void> connectionInstrumenter;
  private final Instrumenter<NabDbRequest, Void> statementInstrumenter;

  public NabTelemetryDataSourceFactory(
      Instrumenter<NabDataSourceInfo, Void> connectionInstrumenter,
      Instrumenter<NabDbRequest, Void> statementInstrumenter
  ) {
    this.connectionInstrumenter = connectionInstrumenter;
    this.statementInstrumenter = statementInstrumenter;
  }

  public DataSource wrap(DataSource dataSourceToWrap) {
    return new NabTelemetryDataSource(dataSourceToWrap, connectionInstrumenter, statementInstrumenter);
  }
}
