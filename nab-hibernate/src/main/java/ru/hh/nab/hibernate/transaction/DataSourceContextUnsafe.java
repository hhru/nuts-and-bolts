package ru.hh.nab.hibernate.transaction;

import static java.util.Optional.ofNullable;
import ru.hh.nab.datasource.DataSourceType;
import ru.hh.nab.common.util.MDC;

import java.util.function.Supplier;

public class DataSourceContextUnsafe {
  static final String MDC_KEY = "db";
  private static final ThreadLocal<DataSourceType> currentDataSourceType = new ThreadLocal<>();

  public static <T> T executeOn(DataSourceType dataSourceType, Supplier<T> supplier) {
    DataSourceType previousDataSourceType = currentDataSourceType.get();
    if (previousDataSourceType == dataSourceType) {
      return supplier.get();
    }

    currentDataSourceType.set(dataSourceType);
    try {
      updateMDC(dataSourceType);
      return supplier.get();
    } finally {
      if (previousDataSourceType == null) {
        currentDataSourceType.remove();
      } else {
        currentDataSourceType.set(previousDataSourceType);
      }
      updateMDC(previousDataSourceType);
    }
  }

  public static DataSourceType getDataSourceType() {
    return currentDataSourceType.get();
  }

  static void setDefaultMDC() {
    updateMDC(DataSourceType.MASTER);
  }

  static void clearMDC() {
    MDC.deleteKey(MDC_KEY);
  }

  private static void updateMDC(DataSourceType dataSourceType) {
    MDC.setKey(MDC_KEY, ofNullable(dataSourceType).orElse(DataSourceType.MASTER).getName());
  }

  private DataSourceContextUnsafe() {
  }
}
