package ru.hh.nab.hibernate.transaction;

import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.hh.nab.datasource.DataSourceType;

import java.util.function.Supplier;

public class DataSourceContext {
  private static boolean checkTransaction = true;

  public static <T> T onReplica(Supplier<T> supplier) {
    return executeOn(DataSourceType.READONLY, supplier);
  }

  public static <T> T onSlowReplica(Supplier<T> supplier) {
    return executeOn(DataSourceType.SLOW, supplier);
  }

  public static <T> T executeOn(String dataSourceName, Supplier<T> supplier) {
    checkSameDataSourceInTransaction(dataSourceName);
    return DataSourceContextUnsafe.executeOn(dataSourceName, false, supplier);
  }

  public static <T> T executeOn(String dataSourceName, boolean overrideByRequestScope, Supplier<T> supplier) {
    checkSameDataSourceInTransaction(dataSourceName);
    return DataSourceContextUnsafe.executeOn(dataSourceName, overrideByRequestScope, supplier);
  }

  private static void checkSameDataSourceInTransaction(String dataSourceName) {
    if (!DataSourceContextUnsafe.getDataSourceKey().equals(dataSourceName)
        && checkTransaction
        && TransactionSynchronizationManager.isActualTransactionActive()) {
      throw new IllegalStateException("Attempt to change data source in transaction");
    }
  }

  public static void disableTransactionCheck() {
    checkTransaction = false;
  }

  static void enableTransactionCheck() {
    checkTransaction = true;
  }

  private DataSourceContext() {}
}
