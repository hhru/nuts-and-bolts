package ru.hh.nab.datasource.healthcheck;

public abstract class HealthCheck extends com.codahale.metrics.health.HealthCheck {
  @Override
  public abstract Result check();
}
