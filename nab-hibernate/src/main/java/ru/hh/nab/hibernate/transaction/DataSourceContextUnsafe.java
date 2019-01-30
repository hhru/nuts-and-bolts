package ru.hh.nab.hibernate.transaction;

import static java.util.Optional.ofNullable;
import javax.annotation.Nonnull;
import ru.hh.nab.datasource.DataSourceType;
import ru.hh.nab.common.mdc.MDC;

import java.util.function.Supplier;

public class DataSourceContextUnsafe {
  static final String MDC_KEY = "db";
  private static final ThreadLocal<String> currentDataSourceType = new ThreadLocal<>();

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

  @Nonnull
  public static String getDataSourceType() {
    return ofNullable(currentDataSourceType.get()).orElse(DataSourceType.MASTER);
  }

  public static void setDefaultMDC() {
    updateMDC(DataSourceType.MASTER);
  }

  public static void clearMDC() {
    MDC.deleteKey(MDC_KEY);
  }

  private static void updateMDC(String dataSourceName) {
    MDC.setKey(MDC_KEY, ofNullable(dataSourceName).orElse(DataSourceType.MASTER));
  }

  private DataSourceContextUnsafe() {
  }
}
