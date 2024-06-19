package ru.hh.nab.datasource;

public interface DatabaseSwitcher {
  String createDataSourceName(String databaseName, String dataSourceType);
  String getDataSourceName(String dataSourceType);
}
