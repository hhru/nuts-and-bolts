package ru.hh.nab.datasource;

import com.zaxxer.hikari.HikariConfig;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class DataSourceType {
  public static final String MASTER = "master";
  public static final String READONLY = "readonly";
  public static final String SLOW = "slow";

  private static final ConcurrentMap<String, DataSourceProperties> PROPERTIES_STORAGE = new ConcurrentHashMap<>();

  private DataSourceType() {
  }

  public static void registerPropertiesFor(String dataSourceName, DataSourceProperties dataSource) {
    PROPERTIES_STORAGE.putIfAbsent(dataSourceName, dataSource);
  }

  static void registerPropertiesFor(HikariConfig hikariConfig) {
    PROPERTIES_STORAGE.putIfAbsent(hikariConfig.getPoolName(), new DataSourceProperties(hikariConfig.isReadOnly()));
  }

  public static DataSourceProperties getPropertiesFor(String dataSourceName) {
    //MASTER=default -> properties must be null-safe
    return PROPERTIES_STORAGE.getOrDefault(dataSourceName, DataSourceProperties.DEFAULT_PROPERTIES);
  }

  public static void clear() {
    PROPERTIES_STORAGE.clear();
  }

  public static final class DataSourceProperties {

    private static final DataSourceProperties DEFAULT_PROPERTIES = new DataSourceProperties(false);

    private final boolean readOnly;

    public DataSourceProperties(boolean readOnly) {
      this.readOnly = readOnly;
    }

    public boolean isReadOnly() {
      return readOnly;
    }
  }
}
