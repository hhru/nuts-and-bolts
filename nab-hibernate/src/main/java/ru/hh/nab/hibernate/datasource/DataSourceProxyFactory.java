package ru.hh.nab.hibernate.datasource;

import javax.sql.DataSource;

public interface DataSourceProxyFactory {

  DataSource createProxy(DataSource original);
}
