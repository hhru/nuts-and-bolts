package ru.hh.nab.jdbc.common.healthcheck;

public interface HealthCheckDataSource {
  String getDataSourceName();
  HealthCheck getHealthCheck();
}
