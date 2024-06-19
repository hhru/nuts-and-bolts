package ru.hh.nab.telemetry.jdbc.internal.datasource;

import javax.sql.DataSource;
import ru.hh.nab.jdbc.common.datasource.DelegatingDataSource;

public class TestDataSource extends DelegatingDataSource {

  public TestDataSource(DataSource delegate) {
    super(delegate);
  }
}
