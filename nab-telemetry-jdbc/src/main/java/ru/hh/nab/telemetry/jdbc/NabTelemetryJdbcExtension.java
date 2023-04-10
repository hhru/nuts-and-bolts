package ru.hh.nab.telemetry.jdbc;

import javax.sql.DataSource;
import ru.hh.nab.datasource.ext.OpenTelemetryJdbcExtension;

public class NabTelemetryJdbcExtension implements OpenTelemetryJdbcExtension {

  private final NabTelemetryDataSourceFactory nabTelemetryDataSourceFactory;

  public NabTelemetryJdbcExtension(NabTelemetryDataSourceFactory nabTelemetryDataSourceFactory) {
    this.nabTelemetryDataSourceFactory = nabTelemetryDataSourceFactory;
  }

  @Override
  public DataSource wrap(DataSource dataSourceToWrap) {
    return nabTelemetryDataSourceFactory.wrap(dataSourceToWrap);
  }
}
