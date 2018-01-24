package ru.hh.nab.hibernate.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.lang.Nullable;
import ru.hh.nab.hibernate.transaction.DataSourceContext;

import javax.sql.DataSource;

public class RoutingDataSource extends AbstractRoutingDataSource {

  private DataSourceProxyFactory proxyFactory;

  public void setProxyFactory(DataSourceProxyFactory proxyFactory) {
    this.proxyFactory = proxyFactory;
  }

  @Override
  protected Object determineCurrentLookupKey() {
    return DataSourceContext.getDataSourceType();
  }

  @Nullable
  @Override
  protected DataSource determineTargetDataSource() {
    DataSource original = super.determineTargetDataSource();
    return proxyFactory != null ? proxyFactory.createProxy(original) : original;
  }
}
