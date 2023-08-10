package ru.hh.nab.hibernate.transaction;

import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.hh.nab.datasource.DataSourceContextUnsafe;
import ru.hh.nab.datasource.DataSourceType;

public class DataSourceContext {
  private static boolean checkTransaction = true;

  public static <T> T onReplica(Supplier<T> supplier) {
    return onDataSource(DataSourceType.READONLY, supplier);
  }

  public static <T> T onSlowReplica(Supplier<T> supplier) {
    return onDataSource(DataSourceType.SLOW, supplier);
  }

  /**
   * @deprecated Use {@link DataSourceContext#onDataSource(String, Supplier) instead}
   */
  @Deprecated
  public static <T> T executeOn(String dataSourceName, Supplier<T> supplier) {
    return onDataSource(dataSourceName, supplier);
  }

  /**
   * @deprecated Use {@link DataSourceContext#onDataSource(String, boolean, Supplier) instead}
   */
  @Deprecated
  public static <T> T executeOn(String dataSourceName, boolean overrideByRequestScope, Supplier<T> supplier) {
    return onDataSource(dataSourceName, overrideByRequestScope, supplier);
  }

  public static void onDataSource(String dataSourceName, Runnable runnable) {
    onDataSource(dataSourceName, false, runnable);
  }

  public static void onDataSource(String dataSourceName, boolean overrideByRequestScope, Runnable runnable) {
    onDataSource(dataSourceName, overrideByRequestScope, () -> {
      runnable.run();
      return null;
    });
  }

  public static <T> T onDataSource(String dataSourceName, Supplier<T> supplier) {
    return onDataSource(dataSourceName, false, supplier);
  }

  public static <T> T onDataSource(String dataSourceName, boolean overrideByRequestScope, Supplier<T> supplier) {
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
