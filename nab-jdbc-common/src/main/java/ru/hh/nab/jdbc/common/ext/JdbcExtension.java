package ru.hh.nab.jdbc.common.ext;

import javax.sql.DataSource;

@FunctionalInterface
public interface JdbcExtension {

  DataSource wrap(DataSource dataSourceToWrap);
}
