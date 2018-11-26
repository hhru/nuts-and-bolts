package ru.hh.nab.datasource;

public enum DataSourceType {
  MASTER("master", false),
  READONLY("readonly", true),
  SLOW("slow", true);

  private final String name;
  private final boolean readonly;

  DataSourceType(String name, boolean readonly) {
    this.name = name;
    this.readonly = readonly;
  }

  public String getName() {
    return name;
  }

  public boolean isReadonly() {
    return readonly;
  }
}
