package ru.hh.nab.datasource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isCurrentTransactionReadOnly;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive;

public final class TransactionAssertions {

  private TransactionAssertions() {
  }

  public static void assertReadWriteTransaction() {
    assertTrue(isSynchronizationActive());
    assertTrue(isActualTransactionActive());
    assertFalse(isCurrentTransactionReadOnly());
  }

  public static void assertReadOnlyTransaction() {
    assertTrue(isSynchronizationActive());
    assertFalse(isActualTransactionActive()); // means no transaction when @Transactional(readOnly=true) is used
  }

  public static void assertActualTransactionIsNotActive() {
    assertFalse(isSynchronizationActive());
    assertFalse(isActualTransactionActive());
  }
}
