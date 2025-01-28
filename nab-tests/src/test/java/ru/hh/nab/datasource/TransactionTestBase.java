package ru.hh.nab.datasource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isCurrentTransactionReadOnly;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive;

public class TransactionTestBase {

  protected static void assertReadWriteTransaction() {
    assertTrue(isSynchronizationActive());
    assertTrue(isActualTransactionActive());
    assertFalse(isCurrentTransactionReadOnly());
  }

  protected static void assertReadOnlyTransaction() {
    assertTrue(isSynchronizationActive());
    assertFalse(isActualTransactionActive()); // means no transaction when @Transactional(readOnly=true) is used
  }

  protected static void assertActualTransactionIsNotActive() {
    assertFalse(isSynchronizationActive());
    assertFalse(isActualTransactionActive());
  }
}
