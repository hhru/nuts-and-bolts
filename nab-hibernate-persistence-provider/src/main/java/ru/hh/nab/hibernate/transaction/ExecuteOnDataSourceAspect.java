package ru.hh.nab.hibernate.transaction;

import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_NOT_SUPPORTED;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRED;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.hh.nab.datasource.DataSourceContextUnsafe;

@Aspect
public class ExecuteOnDataSourceAspect {

  private final DataSourceContextTransactionManager defaultTxManager;
  private final Map<String, DataSourceContextTransactionManager> txManagers;

  public ExecuteOnDataSourceAspect(
      DataSourceContextTransactionManager defaultTxManager,
      Map<String, DataSourceContextTransactionManager> txManagers
  ) {
    this.defaultTxManager = defaultTxManager;
    this.txManagers = txManagers;
  }

  @Around(value = "@annotation(executeOnDataSource)", argNames = "pjp,executeOnDataSource")
  public Object executeOnSpecialDataSource(final ProceedingJoinPoint pjp, final ExecuteOnDataSource executeOnDataSource) throws Throwable {
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
          () -> transactionTemplate.execute(new ExecuteOnDataSourceTransactionCallback(
              pjp, transactionManager.getEntityManagerProxy(), executeOnDataSource.cacheMode()
          )));
    } catch (ExecuteOnDataSourceWrappedException e) {
      throw e.getCause();
    }
  }

  private DataSourceContextTransactionManager getTxManagerByQualifier(String txManagerQualifier) {
    return Optional
        .ofNullable(txManagers.get(txManagerQualifier))
        .orElseThrow(() -> new IllegalStateException("TransactionManager <" + txManagerQualifier + "> is not found"));
  }
}
