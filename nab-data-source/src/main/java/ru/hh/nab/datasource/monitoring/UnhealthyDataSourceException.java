package ru.hh.nab.datasource.monitoring;

import java.sql.SQLException;

public class UnhealthyDataSourceException extends SQLException {

  private static final String ERROR_MESSAGE = "DataSource %s is unhealthy";

  public UnhealthyDataSourceException(String dataSourceName, Throwable cause) {
    super(String.format(ERROR_MESSAGE, dataSourceName), cause);
  }
}
