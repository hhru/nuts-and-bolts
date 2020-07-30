package ru.hh.nab.hibernate.transaction;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_NOT_SUPPORTED;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRED;

import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@Aspect
public class ExecuteOnDataSourceAspect {

  private final Map<String, DataSourceContextTransactionManager> txManagers;

  public ExecuteOnDataSourceAspect(Map<String, DataSourceContextTransactionManager> txManagers) {
    this.txManagers = txManagers;
  }

  @Around(value = "@annotation(executeOnDataSource)", argNames = "pjp,executeOnDataSource")
  public Object executeOnSpecialDataSource(final ProceedingJoinPoint pjp, final ExecuteOnDataSource executeOnDataSource) throws Throwable {
    String dataSourceName = executeOnDataSource.dataSourceType();
    if (DataSourceContextUnsafe.getDataSourceKey().equals(dataSourceName)
        && TransactionSynchronizationManager.isSynchronizationActive()) {
      return pjp.proceed();
    }
    DataSourceContextTransactionManager transactionManager = Optional.of(executeOnDataSource.txManager())
        .filter(Predicate.not(String::isEmpty))
        .map(txManagers::get)
        .or(() -> Optional.of(txManagers).filter(map -> map.size() == 1).map(map -> map.values().iterator().next()))
        .orElseThrow(() -> new IllegalStateException("TransactionManagers present: " + txManagers.keySet()
            + ". Specify txManager in " + ExecuteOnDataSource.class)
        );
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setPropagationBehavior(executeOnDataSource.writableTx() ? PROPAGATION_REQUIRED : PROPAGATION_NOT_SUPPORTED);
    transactionTemplate.setReadOnly(!executeOnDataSource.writableTx());
    try {
      return DataSourceContextUnsafe.executeOn(dataSourceName, executeOnDataSource.overrideByRequestScope(),
          () -> transactionTemplate.execute(new ExecuteOnDataSourceTransactionCallback(
              pjp, transactionManager.getDelegate().getSessionFactory(), executeOnDataSource
          )));
    } catch (ExecuteOnDataSourceWrappedException e) {
      throw e.getCause();
    }
  }
}
