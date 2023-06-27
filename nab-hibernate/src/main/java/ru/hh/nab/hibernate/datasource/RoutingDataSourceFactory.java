package ru.hh.nab.hibernate.datasource;

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

  public RoutingDataSource create(DataSource defaultDataSource) {
    return new RoutingDataSource(defaultDataSource, serviceName, statsDSender);
  }
}
