package ru.hh.nab.datasource;

import java.util.Optional;
import static java.util.Optional.ofNullable;
import java.util.function.Supplier;
import ru.hh.nab.common.mdc.MDC;
import ru.hh.nab.jdbc.common.DataSourceType;

public final class DataSourceContextUnsafe {
  public static final String MDC_KEY = "db";
  private static final ThreadLocal<String> currentDataSourceName = new ThreadLocal<>();
  private static final ThreadLocal<String> requestScopeDataSourceType = new ThreadLocal<>();
  private static DatabaseSwitcher databaseSwitcher = null;

  public static <T> T executeOn(String dataSourceType, boolean overrideByRequestScope, Supplier<T> supplier) {
    var requestDataSourceType = getRequestDataSourceType();
    if (requestDataSourceType != null && overrideByRequestScope && !dataSourceType.equals(requestDataSourceType)) {
      dataSourceType = requestDataSourceType;
    }
    String dataSourceName = createDataSourceName(dataSourceType);
    var previousDataSourceName = currentDataSourceName.get();
    if (dataSourceName.equals(previousDataSourceName)) {
      return supplier.get();
    }
    currentDataSourceName.set(dataSourceName);
    try {
      updateMDC(dataSourceName);
      return supplier.get();
    } finally {
      if (previousDataSourceName == null) {
        currentDataSourceName.remove();
      } else {
        currentDataSourceName.set(previousDataSourceName);
      }
      updateMDC(previousDataSourceName);
    }
  }

  public static void executeInScope(String dataSourceType, Runnable action) {
    try {
      setRequestScopeDataSourceType(dataSourceType);
      action.run();
    } finally {
      clearRequestScopeDataSourceType();
    }
  }

  public static void setDatabaseSwitcher(DatabaseSwitcher databaseSwitcher) {
    DataSourceContextUnsafe.databaseSwitcher = databaseSwitcher;
  }

  public static Optional<DatabaseSwitcher> getDatabaseSwitcher() {
    return ofNullable(databaseSwitcher);
  }

  public static String getDataSourceName() {
    return ofNullable(currentDataSourceName.get()).orElseGet(() -> createDataSourceName(DataSourceType.MASTER));
  }

  public static boolean isCurrentDataSource(String dataSourceType) {
    return getDataSourceName().equals(createDataSourceName(dataSourceType));
  }

  public static void setDefaultMDC() {
    updateMDC(createDataSourceName(DataSourceType.MASTER));
  }

  public static void clearMDC() {
    MDC.deleteKey(MDC_KEY);
  }

  private static void updateMDC(String dataSourceName) {
    MDC.setKey(MDC_KEY, ofNullable(dataSourceName).orElseGet(() -> createDataSourceName(DataSourceType.MASTER)));
  }

  public static void setRequestScopeDataSourceType(String dataSourceType) {
    requestScopeDataSourceType.set(dataSourceType);
  }

  public static void clearRequestScopeDataSourceType() {
    requestScopeDataSourceType.remove();
  }

  public static String getRequestDataSourceType() {
    return requestScopeDataSourceType.get();
  }

  public static String createDataSourceName(String dataSourceType) {
    return databaseSwitcher == null ? dataSourceType : databaseSwitcher.getDataSourceName(dataSourceType);
  }

  private DataSourceContextUnsafe() {
  }
}
