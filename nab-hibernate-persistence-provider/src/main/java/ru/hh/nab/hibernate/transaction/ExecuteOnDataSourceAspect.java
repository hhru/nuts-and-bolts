package ru.hh.nab.hibernate.transaction;

import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.transaction.PlatformTransactionManager;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_NOT_SUPPORTED;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRED;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.hh.nab.jdbc.annotation.ExecuteOnDataSource;
import ru.hh.nab.jdbc.routing.DataSourceContextUnsafe;

@Aspect
public class ExecuteOnDataSourceAspect {

  private final PlatformTransactionManager defaultTxManager;
  private final Map<String, PlatformTransactionManager> txManagers;
  private final ExecuteOnDataSourceTransactionCallbackFactory transactionCallbackFactory;

  public ExecuteOnDataSourceAspect(
      PlatformTransactionManager defaultTxManager,
      Map<String, PlatformTransactionManager> txManagers,
      @Nullable ExecuteOnDataSourceTransactionCallbackFactory transactionCallbackFactory
  ) {
    this.defaultTxManager = defaultTxManager;
    this.txManagers = txManagers;
    this.transactionCallbackFactory = transactionCallbackFactory == null ? new DefaultTransactionCallbackFactory() : transactionCallbackFactory;
  }

  @Around(value = "@annotation(executeOnDataSource)", argNames = "pjp,executeOnDataSource")
  public Object executeOnSpecialDataSource(final ProceedingJoinPoint pjp, final ExecuteOnDataSource executeOnDataSource) throws Throwable {
    String dataSourceType = executeOnDataSource.dataSourceType();
    if (DataSourceContextUnsafe.isCurrentDataSource(dataSourceType) && TransactionSynchronizationManager.isSynchronizationActive()) {
      return pjp.proceed();
    }
    String txManagerQualifier = executeOnDataSource.txManager();
    PlatformTransactionManager transactionManager = StringUtils.isEmpty(txManagerQualifier) ?
        defaultTxManager :
        getTxManagerByQualifier(txManagerQualifier);
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setPropagationBehavior(executeOnDataSource.writableTx() ? PROPAGATION_REQUIRED : PROPAGATION_NOT_SUPPORTED);
    transactionTemplate.setReadOnly(!executeOnDataSource.writableTx());
    try {
      return DataSourceContextUnsafe.executeOn(dataSourceType, executeOnDataSource.overrideByRequestScope(),
          () -> transactionTemplate.execute(transactionCallbackFactory.create(pjp, executeOnDataSource)));
    } catch (ExecuteOnDataSourceWrappedException e) {
      throw e.getCause();
    }
  }

  private PlatformTransactionManager getTxManagerByQualifier(String txManagerQualifier) {
    return Optional
        .ofNullable(txManagers.get(txManagerQualifier))
        .orElseThrow(() -> new IllegalStateException("TransactionManager <" + txManagerQualifier + "> is not found"));
  }

  private static class DefaultTransactionCallbackFactory implements ExecuteOnDataSourceTransactionCallbackFactory {

    @Override
    public TransactionCallback<Object> create(ProceedingJoinPoint pjp, ExecuteOnDataSource executeOnDataSource) {
      return status -> {
        try {
          return pjp.proceed();
        } catch (RuntimeException | Error e) {
          throw e;
        } catch (Throwable e) {
          throw new ExecuteOnDataSourceWrappedException(e);
        }
      };
    }
  }
}
