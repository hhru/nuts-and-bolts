package ru.hh.nab.jdbc.common;

public interface DatabaseSwitcher {
  String createDataSourceName(String databaseName, String dataSourceType);
  String getDataSourceName(String dataSourceType);
}
