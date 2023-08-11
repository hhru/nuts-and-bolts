package ru.hh.nab.datasource.healthcheck;

public interface HealthCheckDataSource {
  String getDataSourceName();
  HealthCheck getHealthCheck();
}
