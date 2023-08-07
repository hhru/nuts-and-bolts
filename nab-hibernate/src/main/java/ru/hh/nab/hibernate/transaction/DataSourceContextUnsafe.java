package ru.hh.nab.hibernate.transaction;

import static java.util.Optional.ofNullable;
import java.util.function.Supplier;
import ru.hh.nab.common.mdc.MDC;
import ru.hh.nab.datasource.DataSourceType;

public final class DataSourceContextUnsafe {
  static final String MDC_KEY = "db";
  private static final ThreadLocal<String> currentDataSourceKey = new ThreadLocal<>();
  private static final ThreadLocal<String> requestScopeDataSourceKey = new ThreadLocal<>();

  public static <T> T executeOn(String dataSourceKey, boolean overrideByRequestScope, Supplier<T> supplier) {
    var requestDataSourceKey = getRequestDataSourceKey();
    if (requestDataSourceKey != null && overrideByRequestScope && !dataSourceKey.equals(requestDataSourceKey)) {
      dataSourceKey = requestDataSourceKey;
    }
    var previousDataSourceKey = currentDataSourceKey.get();
    if (dataSourceKey.equals(previousDataSourceKey)) {
      return supplier.get();
    }
    currentDataSourceKey.set(dataSourceKey);
    try {
      updateMDC(dataSourceKey);
      return supplier.get();
    } finally {
      if (previousDataSourceKey == null) {
        currentDataSourceKey.remove();
      } else {
        currentDataSourceKey.set(previousDataSourceKey);
      }
      updateMDC(previousDataSourceKey);
    }
  }

  public static void executeInScope(String dataSourceKey, Runnable action) {
    try {
      setRequestScopeDataSourceKey(dataSourceKey);
      action.run();
    } finally {
      clearRequestScopeDataSourceKey();
    }
  }

  public static String getDataSourceKey() {
    return ofNullable(currentDataSourceKey.get()).orElse(DataSourceType.MASTER);
  }

  public static void setDefaultMDC() {
    updateMDC(DataSourceType.MASTER);
  }

  public static void clearMDC() {
    MDC.deleteKey(MDC_KEY);
  }

  private static void updateMDC(String dataSourceKey) {
    MDC.setKey(MDC_KEY, ofNullable(dataSourceKey).orElse(DataSourceType.MASTER));
  }

  public static void setRequestScopeDataSourceKey(String dataSourceKey) {
    requestScopeDataSourceKey.set(dataSourceKey);
  }

  public static void clearRequestScopeDataSourceKey() {
    requestScopeDataSourceKey.remove();
  }

  public static String getRequestDataSourceKey() {
    return requestScopeDataSourceKey.get();
  }

  private DataSourceContextUnsafe() {
  }
}
