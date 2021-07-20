package ru.hh.nab.hibernate.transaction;

import java.util.Map;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_NOT_SUPPORTED;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRED;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Aspect
public class ExecuteOnDataSourceAspect {

  private final DataSourceContextTransactionManager defaultTxManager;
  private final Map<String, DataSourceContextTransactionManager> txManagers;

  public ExecuteOnDataSourceAspect(DataSourceContextTransactionManager defaultTxManager,
                                   Map<String, DataSourceContextTransactionManager> txManagers) {
    this.defaultTxManager = defaultTxManager;
    this.txManagers = txManagers;
  }

  @Around(value = "@annotation(executeOnDataSource)", argNames = "pjp,executeOnDataSource")
  public Object executeOnSpecialDataSource(final ProceedingJoinPoint pjp, final ExecuteOnDataSource executeOnDataSource) throws Throwable {
    String dataSourceName = executeOnDataSource.dataSourceType();
    if (DataSourceContextUnsafe.getDataSourceKey().equals(dataSourceName)
        && TransactionSynchronizationManager.isSynchronizationActive()) {
      return pjp.proceed();
    }
    String txManagerQualifier = executeOnDataSource.txManager();
    DataSourceContextTransactionManager transactionManager = Optional.of(txManagerQualifier)
        .filter(String::isEmpty)
        .map(ignored -> defaultTxManager)
        .orElseGet(() -> Optional.ofNullable(txManagers.get(txManagerQualifier))
            .orElseThrow(() -> new IllegalStateException("TransactionManager <" + txManagerQualifier + "> is not found"))
        );
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setPropagationBehavior(executeOnDataSource.writableTx() ? PROPAGATION_REQUIRED : PROPAGATION_NOT_SUPPORTED);
    transactionTemplate.setReadOnly(!executeOnDataSource.writableTx());
    try {
      return DataSourceContextUnsafe.executeOn(dataSourceName, executeOnDataSource.overrideByRequestScope(),
          () -> transactionTemplate.execute(new ExecuteOnDataSourceTransactionCallback(
              pjp, transactionManager.getSessionFactory(), executeOnDataSource
          )));
    } catch (ExecuteOnDataSourceWrappedException e) {
      throw e.getCause();
    }
  }
}
