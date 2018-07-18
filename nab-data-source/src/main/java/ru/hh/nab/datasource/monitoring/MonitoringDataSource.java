package ru.hh.nab.datasource.monitoring;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.function.IntConsumer;

public final class MonitoringDataSource extends DelegatingDataSource {

  private final String dataSourceName;
  private final IntConsumer connectionGetMsConsumer;
  private final IntConsumer connectionUsageMsConsumer;

  public MonitoringDataSource(DataSource dataSource,
                              String name,
                              IntConsumer connectionGetMsConsumer,
                              IntConsumer connectionUsageMsConsumer) {
    super(dataSource);
    this.dataSourceName = name;
    this.connectionGetMsConsumer = connectionGetMsConsumer;
    this.connectionUsageMsConsumer = connectionUsageMsConsumer;
  }

  @Override
  public Connection getConnection() throws SQLException {
    return getConnection(super::getConnection);
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return getConnection(() -> super.getConnection(username, password));
  }

  private Connection getConnection(Callable<Connection> connectionProvider) throws SQLException{
    Connection connection;
    long start = System.currentTimeMillis();
    try {
      connection = connectionProvider.call();
    } catch (Exception e) {
      throw new SQLException("failed to get connection, data source " + dataSourceName + ", cause " + e.toString(), e);
    } finally {
      long end = System.currentTimeMillis();
      connectionGetMsConsumer.accept((int) (end - start));
    }
    return wrapIntoMonitoring(connection);
  }

  private Connection wrapIntoMonitoring(Connection connection) {
    return new MonitoringConnection(connection, connectionUsageMsConsumer);
  }

}
