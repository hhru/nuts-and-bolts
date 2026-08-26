package ru.hh.nab.datasource.routing;

import javax.sql.DataSource;
import ru.hh.metrics.MetricsSender;

public class RoutingDataSourceFactory {

  private final String serviceName;
  private final MetricsSender metricsSender;

  public RoutingDataSourceFactory(String serviceName, MetricsSender metricsSender) {
    this.serviceName = serviceName;
    this.metricsSender = metricsSender;
  }

  /**
   * It's not allowed to use this factory method if application needs to work with multiple databases.
   * In this case you should use {@link RoutingDataSourceFactory#create()} and inject all dataSources via
   * - {@link RoutingDataSource#addNamedDataSource(DataSource)} - the most preferred way
   * - {@link RoutingDataSource#addDataSource(String, DataSource)}
   * - {@link RoutingDataSource#addDataSource(String, String, DataSource)}
   */
  public RoutingDataSource create(DataSource defaultDataSource) {
    return new RoutingDataSource(defaultDataSource, serviceName, metricsSender);
  }

  public RoutingDataSource create() {
    return new RoutingDataSource(serviceName, metricsSender);
  }
}
