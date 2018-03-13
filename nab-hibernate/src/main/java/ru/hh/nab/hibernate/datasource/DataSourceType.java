package ru.hh.nab.hibernate.datasource;

public enum DataSourceType {

  MASTER("master"),
  READONLY("readonly"),
  SLOW("slow");

  private final String name;

  DataSourceType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
