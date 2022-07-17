package ru.hh.nab.datasource;

import com.zaxxer.hikari.HikariConfig;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class DataSourceType {
  public static final String MASTER = "master";
  public static final String READONLY = "readonly";
  public static final String SLOW = "slow";

  private static final ConcurrentMap<String, DataSourceProperties> PROPERTIES_STORAGE = new ConcurrentHashMap<>();

  private DataSourceType() {
  }

  static void registerPropertiesFor(String dataSourceName, DataSourceProperties dataSourceProperties) {
    PROPERTIES_STORAGE.putIfAbsent(dataSourceName, dataSourceProperties);
  }

  static void registerPropertiesFor(HikariConfig hikariConfig, boolean isReadonly) {
    PROPERTIES_STORAGE.putIfAbsent(hikariConfig.getPoolName(), new DataSourceProperties(!isReadonly));
  }

  static void clear() {
    PROPERTIES_STORAGE.clear();
  }

  public static DataSourceProperties getPropertiesFor(String dataSourceName) {
    //MASTER=default -> properties must be null-safe
    return PROPERTIES_STORAGE.getOrDefault(dataSourceName, DataSourceProperties.DEFAULT_PROPERTIES);
  }

  public static final class DataSourceProperties {

    private static final DataSourceProperties DEFAULT_PROPERTIES = new DataSourceProperties(true);

    private final boolean writable;
    private final String secondaryDataSource;

    public DataSourceProperties(boolean writable) {
      this(writable, null);
    }

    public DataSourceProperties(boolean writable, String secondaryDataSource) {
      this.writable = writable;
      this.secondaryDataSource = secondaryDataSource;
    }

    public boolean isWritable() {
      return writable;
    }

    public Optional<String> getSecondaryDataSource() {
      return Optional.ofNullable(secondaryDataSource);
    }
  }
}
