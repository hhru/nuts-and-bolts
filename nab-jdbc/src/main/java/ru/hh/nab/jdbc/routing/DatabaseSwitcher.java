package ru.hh.nab.jdbc.routing;

public interface DatabaseSwitcher {
  String createDataSourceName(String databaseName, String dataSourceType);
  String getDataSourceName(String dataSourceType);
}
