package ru.hh.nab.hibernate.transaction;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.SessionFactory;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_NOT_SUPPORTED;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRED;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

@Aspect
public class ExecuteOnDataSourceAspect {

  private final Map<String, DataSourceContextTransactionManager> txManagers;
  private final Map<String, SessionFactory> sessionFactories;

  public ExecuteOnDataSourceAspect(Map<String, DataSourceContextTransactionManager> txManagers, Map<String, SessionFactory> sessionFactories) {
    this.txManagers = txManagers;
    this.sessionFactories = sessionFactories;
  }

  @Around(value = "@annotation(executeOnDataSource)", argNames = "pjp,executeOnDataSource")
  public Object executeOnSpecialDataSource(final ProceedingJoinPoint pjp, final ExecuteOnDataSource executeOnDataSource) throws Throwable {
    String dataSourceName = executeOnDataSource.dataSourceType();
    if (DataSourceContextUnsafe.getDataSourceKey().equals(dataSourceName)
        && TransactionSynchronizationManager.isSynchronizationActive()) {
      return pjp.proceed();
    }
    TransactionTemplate transactionTemplate = new TransactionTemplate(txManagers.get(executeOnDataSource.txManager()));
    transactionTemplate.setPropagationBehavior(executeOnDataSource.writableTx() ? PROPAGATION_REQUIRED : PROPAGATION_NOT_SUPPORTED);
    transactionTemplate.setReadOnly(!executeOnDataSource.writableTx());

    try {
      return DataSourceContextUnsafe.executeOn(dataSourceName, executeOnDataSource.overrideByRequestScope(),
          () -> transactionTemplate.execute(new ExecuteOnDataSourceTransactionCallback(
              pjp, sessionFactories.get(executeOnDataSource.sessionFactory()), executeOnDataSource
          )));
    } catch (ExecuteOnDataSourceWrappedException e) {
      throw e.getCause();
    }
  }
}
