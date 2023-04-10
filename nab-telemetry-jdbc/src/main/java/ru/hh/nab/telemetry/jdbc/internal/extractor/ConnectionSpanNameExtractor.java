package ru.hh.nab.telemetry.jdbc.internal.extractor;

import ru.hh.nab.telemetry.jdbc.internal.model.NabDataSourceInfo;

public class ConnectionSpanNameExtractor extends DataSourceNameExtractor {

  final static String GET_CONNECTION = "getConnection";

  @Override
  public String extract(NabDataSourceInfo nabDataSourceInfo) {
    return GET_CONNECTION + " " + super.extract(nabDataSourceInfo);
  }
}
