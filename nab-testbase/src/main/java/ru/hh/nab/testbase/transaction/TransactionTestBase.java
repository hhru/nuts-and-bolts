package ru.hh.nab.testbase.transaction;

import jakarta.inject.Inject;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import ru.hh.nab.datasource.transaction.DataSourceContextTransactionManager;
import ru.hh.nab.testbase.extensions.SpringExtensionWithFailFast;

@ExtendWith(SpringExtensionWithFailFast.class)
public class TransactionTestBase {

  @Inject
  protected DataSourceContextTransactionManager transactionManager;

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
