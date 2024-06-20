package ru.hh.nab.jdbc.routing;

import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.hh.nab.jdbc.DataSourceType;

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
  public static <T> T executeOn(String dataSourceType, Supplier<T> supplier) {
    return onDataSource(dataSourceType, supplier);
  }

  /**
   * @deprecated Use {@link DataSourceContext#onDataSource(String, boolean, Supplier) instead}
   */
  @Deprecated
  public static <T> T executeOn(String dataSourceType, boolean overrideByRequestScope, Supplier<T> supplier) {
    return onDataSource(dataSourceType, overrideByRequestScope, supplier);
  }

  public static void onDataSource(String dataSourceType, Runnable runnable) {
    onDataSource(dataSourceType, false, runnable);
  }

  public static void onDataSource(String dataSourceType, boolean overrideByRequestScope, Runnable runnable) {
    onDataSource(dataSourceType, overrideByRequestScope, () -> {
      runnable.run();
      return null;
    });
  }

  public static <T> T onDataSource(String dataSourceType, Supplier<T> supplier) {
    return onDataSource(dataSourceType, false, supplier);
  }

  public static <T> T onDataSource(String dataSourceType, boolean overrideByRequestScope, Supplier<T> supplier) {
    checkSameDataSourceInTransaction(dataSourceType);
    return DataSourceContextUnsafe.executeOn(dataSourceType, overrideByRequestScope, supplier);
  }

  private static void checkSameDataSourceInTransaction(String dataSourceType) {
    if (!DataSourceContextUnsafe.isCurrentDataSource(dataSourceType)
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
