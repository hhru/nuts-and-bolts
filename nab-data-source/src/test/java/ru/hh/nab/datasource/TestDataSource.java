package ru.hh.nab.datasource;

import org.hsqldb.jdbc.JDBCDataSource;

import javax.sql.DataSource;

public class TestDataSource {

  private static final JDBCDataSource INSTANCE = new JDBCDataSource();
  static {
    INSTANCE.setURL("jdbc:hsqldb:mem:test");
  }

  public static DataSource get() {
    return INSTANCE;
  }
}
