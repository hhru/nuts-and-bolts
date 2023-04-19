package ru.hh.nab.telemetry.jdbc;

import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import javax.sql.DataSource;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.telemetry.jdbc.internal.NabTelemetryDataSource;
import ru.hh.nab.telemetry.jdbc.internal.model.NabDataSourceInfo;
import ru.hh.nab.telemetry.jdbc.internal.model.NabDbRequest;

public class NabTelemetryDataSourceFactory {

  private final FileSettings fileSettings;
  private final Instrumenter<NabDataSourceInfo, Void> connectionInstrumenter;
  private final Instrumenter<NabDbRequest, Void> statementInstrumenter;

  public NabTelemetryDataSourceFactory(
      FileSettings fileSettings,
      Instrumenter<NabDataSourceInfo, Void> connectionInstrumenter,
      Instrumenter<NabDbRequest, Void> statementInstrumenter
  ) {
    this.fileSettings = fileSettings;
    this.connectionInstrumenter = connectionInstrumenter;
    this.statementInstrumenter = statementInstrumenter;
  }

  public DataSource wrap(DataSource dataSource) {
    boolean openTelemetryJdbcEnabled = fileSettings.getBoolean("opentelemetry.jdbc.enabled", false);
    return openTelemetryJdbcEnabled ?
        new NabTelemetryDataSource(dataSource, connectionInstrumenter, statementInstrumenter) :
        dataSource;
  }
}
