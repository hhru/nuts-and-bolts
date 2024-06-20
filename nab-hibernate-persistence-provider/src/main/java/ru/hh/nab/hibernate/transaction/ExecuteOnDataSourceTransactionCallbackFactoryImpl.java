package ru.hh.nab.hibernate.transaction;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.transaction.support.TransactionCallback;
import ru.hh.nab.jdbc.annotation.ExecuteOnDataSource;

public class ExecuteOnDataSourceTransactionCallbackFactoryImpl implements ExecuteOnDataSourceTransactionCallbackFactory {

  private final EntityManager entityManager;

  public ExecuteOnDataSourceTransactionCallbackFactoryImpl(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public TransactionCallback<Object> create(ProceedingJoinPoint pjp, ExecuteOnDataSource executeOnDataSource) {
    return new ExecuteOnDataSourceTransactionCallback(pjp, entityManager, executeOnDataSource);
  }
}
