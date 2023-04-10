package ru.hh.nab.telemetry.jdbc.internal.model;

import io.opentelemetry.instrumentation.jdbc.internal.DbRequest;

public class NabDbRequest {

  private DbRequest dbRequest;
  private NabDataSourceInfo nabDataSourceInfo;

  public DbRequest getDbRequest() {
    return dbRequest;
  }

  public NabDbRequest setDbRequest(DbRequest dbRequest) {
    this.dbRequest = dbRequest;
    return this;
  }

  public NabDataSourceInfo getNabDataSourceInfo() {
    return nabDataSourceInfo;
  }

  public NabDbRequest setNabDataSourceInfo(NabDataSourceInfo nabDataSourceInfo) {
    this.nabDataSourceInfo = nabDataSourceInfo;
    return this;
  }
}
