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

  public static <T> T executeOn(DataSourceType dataSourceType, Supplier<T> supplier) {
    checkSameDataSourceInTransaction(dataSourceType.getName());
    return DataSourceContextUnsafe.executeOn(dataSourceType.getName(), supplier);
  }

  public static <T> T executeOn(String dataSourceName, Supplier<T> supplier) {
    checkSameDataSourceInTransaction(dataSourceName);
    return DataSourceContextUnsafe.executeOn(dataSourceName, supplier);
  }

  private static void checkSameDataSourceInTransaction(String dataSourceName) {
    if (!dataSourceName.equals(DataSourceContextUnsafe.getDataSourceType())
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
