package ru.hh.nab.datasource.ext;

import javax.sql.DataSource;

@FunctionalInterface
public interface JdbcExtension {

  DataSource wrap(DataSource dataSourceToWrap);
}
