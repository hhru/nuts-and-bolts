package ru.hh.nab.testbase.jpa;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import ru.hh.nab.testbase.transaction.TransactionTestBase;

public abstract class JpaTestBase extends TransactionTestBase {
  @Inject
  protected EntityManager entityManager;
}
