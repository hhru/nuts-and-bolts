package ru.hh.nab.hibernate.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import static java.util.Optional.ofNullable;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.lang.Nullable;
import ru.hh.nab.datasource.healthcheck.UnhealthyDataSourceException;
import ru.hh.nab.hibernate.transaction.DataSourceContextUnsafe;
import ru.hh.nab.metrics.Counters;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.Tag;
import static ru.hh.nab.metrics.Tag.APP_TAG_NAME;
import static ru.hh.nab.metrics.Tag.DATASOURCE_TAG_NAME;

public class RoutingDataSource extends AbstractRoutingDataSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(RoutingDataSource.class);
  private static final String SUCCESSFUL_SWITCHING_METRIC_NAME = "nab.db.master.switching.success";
  private static final String FAILED_SWITCHING_METRIC_NAME = "nab.db.master.switching.failure";

  private final Map<String, DataSource> replicas = new HashMap<>();
  private final Tag appTag;
  private final Counters successfulSwitchingCounters, failedSwitchingCounters;
  private DataSourceProxyFactory proxyFactory;

  /**
   * @deprecated Use {@link RoutingDataSourceFactory#create(DataSource)}
   */
  @Deprecated
  public RoutingDataSource(DataSource defaultDataSource) {
    this(defaultDataSource, null, null);
  }

  public RoutingDataSource(DataSource defaultDataSource, String serviceName, StatsDSender statsDSender) {
    setDefaultTargetDataSource(new LazyConnectionDataSourceProxy(defaultDataSource));

    this.appTag = new Tag(APP_TAG_NAME, serviceName);
    this.successfulSwitchingCounters = new Counters(50);
    this.failedSwitchingCounters = new Counters(50);

    ofNullable(statsDSender)
        .ifPresent(sender -> sender.sendPeriodically(() -> {
          statsDSender.sendCounters(SUCCESSFUL_SWITCHING_METRIC_NAME, successfulSwitchingCounters);
          statsDSender.sendCounters(FAILED_SWITCHING_METRIC_NAME, failedSwitchingCounters);
        }));
  }

  @Override
  protected String determineCurrentLookupKey() {
    return DataSourceContextUnsafe.getDataSourceKey();
  }

  @Nullable
  @Override
  protected DataSource determineTargetDataSource() {
    DataSource original = super.determineTargetDataSource();
    return proxyFactory != null ? proxyFactory.createProxy(original) : original;
  }

  @Override
  public Connection getConnection() throws SQLException {
    return this.getConnection(DataSource::getConnection);
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return this.getConnection(dataSource -> dataSource.getConnection(username, password));
  }

  @Override
  public void afterPropertiesSet() {
    setTargetDataSources(new HashMap<>(replicas));
    super.afterPropertiesSet();
  }

  public void addDataSource(String dataSourceName, DataSource dataSource) {
    replicas.put(dataSourceName, dataSource);
  }

  public void setProxyFactory(DataSourceProxyFactory proxyFactory) {
    this.proxyFactory = proxyFactory;
  }

  private Connection getConnection(DataSourceConnectionSupplier dataSourceConnectionSupplier) throws SQLException {
    String dataSourceName = this.determineCurrentLookupKey();
    DataSource targetDataSource = this.determineTargetDataSource();
    try {
      return dataSourceConnectionSupplier.getFor(targetDataSource);
    } catch (UnhealthyDataSourceException e) {
      LOGGER.error("Could not establish connection to unhealthy data source {}", dataSourceName, e);
      DataSource defaultDataSource = super.getResolvedDefaultDataSource();
      if (Objects.equals(targetDataSource, defaultDataSource)) {
        throw e;
      } else {
        Tag[] tags = {appTag, new Tag(DATASOURCE_TAG_NAME, dataSourceName)};
        try {
          Connection connection = dataSourceConnectionSupplier.getFor(defaultDataSource);
          successfulSwitchingCounters.add(1, tags);
          return connection;
        } catch (SQLException ex) {
          failedSwitchingCounters.add(1, tags);
          throw ex;
        }
      }
    }
  }

  @FunctionalInterface
  private interface DataSourceConnectionSupplier {
    Connection getFor(DataSource dataSource) throws SQLException;
  }
}
