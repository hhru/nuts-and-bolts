package ru.hh.nab.datasource.aspect;

import java.util.Map;
import static java.util.Objects.requireNonNull;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_NOT_SUPPORTED;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRED;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.hh.nab.datasource.annotation.ExecuteOnDataSource;
import ru.hh.nab.datasource.routing.DataSourceContextUnsafe;
import ru.hh.nab.datasource.transaction.DataSourceContextTransactionManager;

@Aspect
public class ExecuteOnDataSourceAspect {

  private final boolean skip;
  private final DataSourceContextTransactionManager defaultTxManager;
  private final Map<String, DataSourceContextTransactionManager> txManagers;

  private ExecuteOnDataSourceAspect() {
    skip = true;
    this.defaultTxManager = null;
    this.txManagers = null;
  }

  public ExecuteOnDataSourceAspect(
      DataSourceContextTransactionManager defaultTxManager,
      Map<String, DataSourceContextTransactionManager> txManagers
  ) {
    skip = false;
    this.defaultTxManager = requireNonNull(defaultTxManager);
    this.txManagers = requireNonNull(txManagers);
  }

  public static ExecuteOnDataSourceAspect skipped() {
    return new ExecuteOnDataSourceAspect();
  }

  @Around(value = "@annotation(executeOnDataSource)", argNames = "pjp,executeOnDataSource")
  public Object executeOnSpecialDataSource(final ProceedingJoinPoint pjp, final ExecuteOnDataSource executeOnDataSource) throws Throwable {
    if (skip) {
      return pjp.proceed();
    }

    String dataSourceType = executeOnDataSource.dataSourceType();
    if (DataSourceContextUnsafe.isCurrentDataSource(dataSourceType) && TransactionSynchronizationManager.isSynchronizationActive()) {
      return pjp.proceed();
    }
    String txManagerQualifier = executeOnDataSource.txManager();
    DataSourceContextTransactionManager transactionManager = StringUtils.isEmpty(txManagerQualifier) ?
        defaultTxManager :
        getTxManagerByQualifier(txManagerQualifier);
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setPropagationBehavior(executeOnDataSource.writableTx() ? PROPAGATION_REQUIRED : PROPAGATION_NOT_SUPPORTED);
    transactionTemplate.setReadOnly(!executeOnDataSource.writableTx());
    try {
      return DataSourceContextUnsafe.executeOn(dataSourceType, executeOnDataSource.overrideByRequestScope(),
          () -> transactionTemplate.execute(transactionStatus -> doInTransaction(pjp)));
    } catch (ExecuteOnDataSourceWrappedException e) {
      throw e.getCause();
    }
  }

  private DataSourceContextTransactionManager getTxManagerByQualifier(String txManagerQualifier) {
    return Optional
        .ofNullable(txManagers.get(txManagerQualifier))
        .orElseThrow(() -> new IllegalStateException("TransactionManager <" + txManagerQualifier + "> is not found"));
  }

  private static Object doInTransaction(ProceedingJoinPoint pjp) {
    try {
      return pjp.proceed();
    } catch (RuntimeException | Error e) {
      throw e;
    } catch (Throwable e) {
      throw new ExecuteOnDataSourceWrappedException(e);
    }
  }
}
