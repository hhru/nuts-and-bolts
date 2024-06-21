package ru.hh.nab.testbase.jpa;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@ExtendWith(SpringExtension.class)
public abstract class JpaTestBase {
  @Inject
  protected EntityManager entityManager;
  @Inject
  protected PlatformTransactionManager transactionManager;

  private static TransactionStatus transactionStatus;

  protected void startTransaction() {
    DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
    transactionDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transactionStatus = transactionManager.getTransaction(transactionDefinition);
  }

  protected void commitTransaction() {
    transactionManager.commit(transactionStatus);
    transactionStatus = null;
  }

  protected void rollBackTransaction() {
    transactionManager.rollback(transactionStatus);
    transactionStatus = null;
  }
}
