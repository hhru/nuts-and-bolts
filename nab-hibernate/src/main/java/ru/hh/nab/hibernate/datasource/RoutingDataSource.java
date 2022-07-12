package ru.hh.nab.hibernate.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.lang.Nullable;
import ru.hh.nab.datasource.healthcheck.UnhealthyDataSourceException;
import ru.hh.nab.hibernate.transaction.DataSourceContextUnsafe;

public class RoutingDataSource extends AbstractRoutingDataSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(RoutingDataSource.class);

  private final Map<String, DataSource> replicas = new HashMap<>();
  private DataSourceProxyFactory proxyFactory;

  public RoutingDataSource(DataSource defaultDataSource) {
    setDefaultTargetDataSource(new LazyConnectionDataSourceProxy(defaultDataSource));
  }

  @Override
  protected Object determineCurrentLookupKey() {
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
    DataSource targetDataSource = this.determineTargetDataSource();
    try {
      return dataSourceConnectionSupplier.getFor(targetDataSource);
    } catch (UnhealthyDataSourceException e) {
      LOGGER.error("Could not establish connection to unhealthy data source", e);
      DataSource defaultDataSource = super.getResolvedDefaultDataSource();
      if (defaultDataSource.equals(targetDataSource)) {
        throw e;
      } else {
        return dataSourceConnectionSupplier.getFor(defaultDataSource);
      }
    }
  }

  @FunctionalInterface
  private interface DataSourceConnectionSupplier {
    Connection getFor(DataSource dataSource) throws SQLException;
  }
}
