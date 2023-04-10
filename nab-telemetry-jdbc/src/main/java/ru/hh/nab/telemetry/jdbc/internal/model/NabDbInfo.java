package ru.hh.nab.telemetry.jdbc.internal.model;

import io.opentelemetry.instrumentation.jdbc.internal.dbinfo.DbInfo;

public class NabDbInfo {

  private DbInfo dbInfo;
  private NabDataSourceInfo nabDataSourceInfo;

  public DbInfo getDbInfo() {
    return dbInfo;
  }

  public NabDbInfo setDbInfo(DbInfo dbInfo) {
    this.dbInfo = dbInfo;
    return this;
  }

  public NabDataSourceInfo getNabDataSourceInfo() {
    return nabDataSourceInfo;
  }

  public NabDbInfo setNabDataSourceInfo(NabDataSourceInfo nabDataSourceInfo) {
    this.nabDataSourceInfo = nabDataSourceInfo;
    return this;
  }
}
