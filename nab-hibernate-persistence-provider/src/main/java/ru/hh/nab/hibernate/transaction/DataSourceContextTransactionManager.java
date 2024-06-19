package ru.hh.nab.hibernate.transaction;

import org.springframework.orm.jpa.EntityManagerProxy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_NOT_SUPPORTED;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_SUPPORTS;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive;
import ru.hh.nab.datasource.DataSourceContextUnsafe;
import ru.hh.nab.jdbc.common.DataSourcePropertiesStorage;

public class DataSourceContextTransactionManager implements PlatformTransactionManager {

  private final PlatformTransactionManager delegate;
  private final EntityManagerProxy entityManagerProxy;

  public DataSourceContextTransactionManager(PlatformTransactionManager delegate, EntityManagerProxy entityManagerProxy) {
    this.delegate = delegate;
    this.entityManagerProxy = entityManagerProxy;
  }

  public EntityManagerProxy getEntityManagerProxy() {
    return entityManagerProxy;
  }

  @Override
  public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
    TransactionDefinition fixedDefinition = fixTransactionDefinition(definition);
    return delegate.getTransaction(fixedDefinition);
  }

  private static TransactionDefinition fixTransactionDefinition(TransactionDefinition definition) {
    if (DataSourcePropertiesStorage.isWritableDataSource(DataSourceContextUnsafe.getDataSourceName())) {
      if (definition.isReadOnly()) {
        return getReadOnlyTransactionDefinition(definition, PROPAGATION_SUPPORTS);
      }
      if (isSynchronizationActive() && !isActualTransactionActive()) {
        // we do not open read-write transaction after @Transaction(readonly) was used;
        // instead, we force read-only pseudo-transaction to avoid problems with multiple synchronizations.
        return getReadOnlyTransactionDefinition(definition, PROPAGATION_SUPPORTS);
      }
      return definition;
    }

    if (definition.getPropagationBehavior() == PROPAGATION_NOT_SUPPORTED && definition.isReadOnly()) {
      return definition;
    }

    return getReadOnlyTransactionDefinition(definition, PROPAGATION_NOT_SUPPORTED);
  }

  @Override
  public void commit(TransactionStatus status) throws TransactionException {
    delegate.commit(status);
  }

  @Override
  public void rollback(TransactionStatus status) throws TransactionException {
    delegate.rollback(status);
  }

  private static DefaultTransactionDefinition getReadOnlyTransactionDefinition(TransactionDefinition currentDefinition, int propagationBehavior) {
    DefaultTransactionDefinition definition = new DefaultTransactionDefinition(currentDefinition);
    definition.setPropagationBehavior(propagationBehavior);
    definition.setReadOnly(true);
    return definition;
  }
}
