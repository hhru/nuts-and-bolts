package ru.hh.nab.hibernate.transaction;

import java.util.List;
import javax.sql.DataSource;

/**
 * Class is meant to provide a reliable point in bean's graph
 * where initialization performed for & by DataSources completed.
 */
public class DataSourcesReadyTarget {

  private final List<DataSource> dataSources;

  public DataSourcesReadyTarget(List<DataSource> dataSources) {
    this.dataSources = dataSources;
  }
}
