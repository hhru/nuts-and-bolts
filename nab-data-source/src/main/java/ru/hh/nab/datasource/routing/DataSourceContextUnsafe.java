package ru.hh.nab.datasource.routing;

import java.util.Optional;
import static java.util.Optional.ofNullable;
import java.util.function.Supplier;
import ru.hh.nab.common.mdc.MDC;
import ru.hh.nab.datasource.DataSourceType;

public final class DataSourceContextUnsafe {
  public static final String MDC_KEY = "db";
  private static final ThreadLocal<String> defaultDataSourceName = new ThreadLocal<>();
  private static final ThreadLocal<String> currentDataSourceName = new ThreadLocal<>();
  private static final ThreadLocal<String> requestScopeDataSourceType = new ThreadLocal<>();
  private static DatabaseSwitcher databaseSwitcher = null;

  public static <T> T executeOn(String dataSourceType, boolean overrideByRequestScope, Supplier<T> supplier) {
    var requestDataSourceType = getRequestDataSourceType();
    if (requestDataSourceType != null && overrideByRequestScope && !dataSourceType.equals(requestDataSourceType)) {
      dataSourceType = requestDataSourceType;
    }
    String dataSourceName = createDataSourceName(dataSourceType);
    return executeOn(dataSourceName, supplier);
  }

  public static <T> T executeOn(String dataSourceName, Supplier<T> supplier) {
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
        clearMDC();
      } else {
        currentDataSourceName.set(previousDataSourceName);
        updateMDC(previousDataSourceName);
      }
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

  public static void executeWithDefaultDataSource(String dataSourceType, Runnable action) {
    try {
      String dataSourceName = createDataSourceName(dataSourceType);
      defaultDataSourceName.set(dataSourceName);
      action.run();
    } finally {
      defaultDataSourceName.remove();
    }
  }

  public static void setDatabaseSwitcher(DatabaseSwitcher databaseSwitcher) {
    DataSourceContextUnsafe.databaseSwitcher = databaseSwitcher;
  }

  public static Optional<DatabaseSwitcher> getDatabaseSwitcher() {
    return ofNullable(databaseSwitcher);
  }

  public static String getDataSourceName() {
    return ofNullable(currentDataSourceName.get())
        .or(() -> Optional.ofNullable(defaultDataSourceName.get()))
        .orElseGet(() -> createDataSourceName(DataSourceType.MASTER));
  }

  public static boolean isCurrentDataSource(String dataSourceType) {
    return getDataSourceName().equals(createDataSourceName(dataSourceType));
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

  private static void clearMDC() {
    MDC.deleteKey(MDC_KEY);
  }

  private static void updateMDC(String dataSourceName) {
    MDC.setKey(MDC_KEY, dataSourceName);
  }

  private DataSourceContextUnsafe() {
  }
}
