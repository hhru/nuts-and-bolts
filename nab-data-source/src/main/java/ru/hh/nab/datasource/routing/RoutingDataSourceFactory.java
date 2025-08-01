package ru.hh.nab.datasource.routing;

import javax.sql.DataSource;
import ru.hh.nab.metrics.StatsDSender;

public class RoutingDataSourceFactory {

  private final String serviceName;
  private final StatsDSender statsDSender;

  public RoutingDataSourceFactory(String serviceName, StatsDSender statsDSender) {
    this.serviceName = serviceName;
    this.statsDSender = statsDSender;
  }

  /**
   * It's not allowed to use this factory method if application needs to work with multiple databases.
   * In this case you should use {@link RoutingDataSourceFactory#create()} and inject all dataSources via
   * - {@link RoutingDataSource#addNamedDataSource(DataSource)} - the most preferred way
   * - {@link RoutingDataSource#addDataSource(String, DataSource)}
   * - {@link RoutingDataSource#addDataSource(String, String, DataSource)}
   */
  public RoutingDataSource create(DataSource defaultDataSource) {
    return new RoutingDataSource(defaultDataSource, serviceName, statsDSender);
  }

  public RoutingDataSource create() {
    return new RoutingDataSource(serviceName, statsDSender);
  }
}
