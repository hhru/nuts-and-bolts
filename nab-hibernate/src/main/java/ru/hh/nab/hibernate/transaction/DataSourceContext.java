package ru.hh.nab.hibernate.transaction;

import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.hh.nab.hibernate.datasource.DataSourceType;
import ru.hh.nab.core.util.MDC;

import java.util.function.Supplier;

import static ru.hh.nab.core.util.MDC.DATA_SOURCE_MDC_KEY;

public class DataSourceContext {
  private static final ThreadLocal<DataSourceType> CURRENT_DATA_SOURCE_TYPE = new ThreadLocal<>();
  private static boolean checkNoTransaction = true;

  public static <T> T onReplica(Supplier<T> supplier) {
    return executeOn(DataSourceType.REPLICA, supplier);
  }

  private static <T> T executeOn(DataSourceType dataSourceType, Supplier<T> supplier) {
    checkSameDataSourceInTransaction(dataSourceType);

    DataSourceType prevDataSourceType = CURRENT_DATA_SOURCE_TYPE.get();
    if (prevDataSourceType == dataSourceType) {
      return supplier.get();
    }

    CURRENT_DATA_SOURCE_TYPE.set(dataSourceType);
    try {
      updateMDC(dataSourceType);
      return supplier.get();
    } finally {
      if (prevDataSourceType == null) {
        CURRENT_DATA_SOURCE_TYPE.remove();
      } else {
        CURRENT_DATA_SOURCE_TYPE.set(prevDataSourceType);
      }
      updateMDC(prevDataSourceType);
    }
  }

  private static void checkSameDataSourceInTransaction(DataSourceType dataSourceType) {
    if (TransactionSynchronizationManager.isActualTransactionActive()
      && checkNoTransaction
      && dataSourceType != getDataSourceType()) {
      throw new IllegalStateException("Attempt to change data source in transaction");
    }
  }

  public static DataSourceType getDataSourceType() {
    return CURRENT_DATA_SOURCE_TYPE.get();
  }

  static void setDefaultMDC() {
    updateMDC(DataSourceType.DEFAULT);
  }

  static void clearMDC() {
    MDC.deleteKey(DATA_SOURCE_MDC_KEY);
  }

  private static void updateMDC(DataSourceType dataSourceType) {
    MDC.setKey(DATA_SOURCE_MDC_KEY, dataSourceType != null ? dataSourceType.getId() : DataSourceType.DEFAULT.getId());
  }

  static void disableTransactionCheck() {
    checkNoTransaction = false;
  }

  static void enableTransactionCheck() {
    checkNoTransaction = true;
  }

  private DataSourceContext() {
  }
}
