package ru.hh.nab.jdbc.common;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DataSourcePropertiesStorage {

  private static final ConcurrentMap<String, DataSourceProperties> PROPERTIES_STORAGE = new ConcurrentHashMap<>();

  public static void registerPropertiesFor(String dataSourceName, DataSourceProperties dataSourceProperties) {
    PROPERTIES_STORAGE.putIfAbsent(dataSourceName, dataSourceProperties);
  }

  public static void clear() {
    PROPERTIES_STORAGE.clear();
  }

  public static boolean isConfigured(String dataSourceName) {
    return PROPERTIES_STORAGE.get(dataSourceName) != null;
  }

  public static boolean isWritableDataSource(String dataSourceName) {
    return getPropertiesFor(dataSourceName).isWritable();
  }

  public static Optional<String> getSecondaryDataSourceName(String primaryDataSourceName) {
    return getPropertiesFor(primaryDataSourceName).getSecondaryDataSource();
  }

  private static DataSourceProperties getPropertiesFor(String dataSourceName) {
    //MASTER=default -> properties must be null-safe
    return PROPERTIES_STORAGE.getOrDefault(dataSourceName, DataSourceProperties.DEFAULT_PROPERTIES);
  }

  private DataSourcePropertiesStorage() {
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
