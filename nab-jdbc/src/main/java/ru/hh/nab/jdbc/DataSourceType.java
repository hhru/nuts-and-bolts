package ru.hh.nab.jdbc;

public final class DataSourceType {
  public static final String MASTER = "master";
  public static final String READONLY = "readonly";
  public static final String SLOW = "slow";

  private DataSourceType() {
  }
}
