package ru.hh.nab.datasource.jdbc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A {@link javax.sql.DataSource} that sends Postgresql statement timeout query after obtaining a connection from the underlying datasource.<br/>
 * Insert between driver datasource and pooling datasource, so that statement timeout query is executed only once
 * and all subsequent queries are bounded by the timeout.<br/>
 * Do not use in front of other connection pools such as PgBouncer, because the timeout can affect other applications that share the same PgBouncer.
 */
public class StatementTimeoutDataSource extends DelegatingDataSource {

  private final String setStatementTimeoutQuery;

  public StatementTimeoutDataSource(DataSource delegate, int statementTimeoutMs) {
    super(delegate);
    this.setStatementTimeoutQuery = "SET STATEMENT_TIMEOUT TO " + statementTimeoutMs;
  }

  @Override
  public Connection getConnection() throws SQLException {
    Connection connection = super.getConnection();
    setStatementTimeout(connection);
    return connection;
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    Connection connection = super.getConnection(username, password);
    setStatementTimeout(connection);
    return connection;
  }

  private void setStatementTimeout(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(setStatementTimeoutQuery);
    }
  }
}
