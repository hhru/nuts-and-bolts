package ru.hh.nab.jdbc.routing;

import jakarta.inject.Named;
import javax.sql.DataSource;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import ru.hh.nab.metrics.StatsDSender;

public class RoutingDataSourceFactory {

  private final String serviceName;
  private final StatsDSender statsDSender;

  public RoutingDataSourceFactory(@Named(SERVICE_NAME) String serviceName, StatsDSender statsDSender) {
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
