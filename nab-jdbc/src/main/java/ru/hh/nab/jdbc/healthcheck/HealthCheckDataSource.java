package ru.hh.nab.jdbc.healthcheck;

public interface HealthCheckDataSource {
  String getDataSourceName();
  HealthCheck getHealthCheck();
}
