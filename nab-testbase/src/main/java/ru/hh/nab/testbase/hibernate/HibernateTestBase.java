package ru.hh.nab.testbase.hibernate;

import javax.inject.Inject;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import ru.hh.nab.hibernate.transaction.DataSourceContextTransactionManager;
import ru.hh.nab.testbase.extensions.SpringExtensionWithFailFast;

@ExtendWith(SpringExtensionWithFailFast.class)
public abstract class HibernateTestBase {
  @Inject
  protected SessionFactory sessionFactory;
  @Inject
  protected DataSourceContextTransactionManager transactionManager;

  private static TransactionStatus transactionStatus;

  protected void startTransaction() {
    DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
    transactionDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transactionStatus = transactionManager.getTransaction(transactionDefinition);
  }

  protected void rollBackTransaction() {
    transactionManager.rollback(transactionStatus);
    transactionStatus = null;
  }

  protected Session getCurrentSession() {
    return sessionFactory.getCurrentSession();
  }
}
