package ru.hh.nab.testbase.old.hibernate;

import javax.inject.Inject;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import ru.hh.nab.hibernate.transaction.DataSourceContextTransactionManager;

/**
 * use junit 5 version from nab-testbase
 * */
@Deprecated
public abstract class HibernateTestBase extends AbstractJUnit4SpringContextTests {
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
