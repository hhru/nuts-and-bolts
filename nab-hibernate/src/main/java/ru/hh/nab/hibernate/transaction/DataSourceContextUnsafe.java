package ru.hh.nab.hibernate.transaction;

import static java.util.Optional.ofNullable;
import ru.hh.nab.datasource.DataSourceType;
import ru.hh.nab.common.mdc.MDC;

import java.util.function.Supplier;

public class DataSourceContextUnsafe {
  static final String MDC_KEY = "db";
  private static final ThreadLocal<String> currentDataSourceType = new ThreadLocal<>();

  public static <T> T executeOn(DataSourceType dataSourceType, Supplier<T> supplier) {
    return executeOn(dataSourceType.getName(), supplier);
  }

  public static <T> T executeOn(String dataSourceName, Supplier<T> supplier) {
    String previousDataSourceName = currentDataSourceType.get();
    if (dataSourceName.equals(previousDataSourceName)) {
      return supplier.get();
    }

    currentDataSourceType.set(dataSourceName);
    try {
      updateMDC(dataSourceName);
      return supplier.get();
    } finally {
      if (previousDataSourceName == null) {
        currentDataSourceType.remove();
      } else {
        currentDataSourceType.set(previousDataSourceName);
      }
      updateMDC(previousDataSourceName);
    }
  }

  public static String getDataSourceType() {
    return currentDataSourceType.get();
  }

  public static void setDefaultMDC() {
    updateMDC(DataSourceType.MASTER.getName());
  }

  public static void clearMDC() {
    MDC.deleteKey(MDC_KEY);
  }

  private static void updateMDC(String dataSourceName) {
    MDC.setKey(MDC_KEY, ofNullable(dataSourceName).orElseGet(DataSourceType.MASTER::getName));
  }

  private DataSourceContextUnsafe() {
  }
}
