package ru.hh.nab.testbase.jpa;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.hh.nab.testbase.transaction.TransactionTestBase;

@ExtendWith(SpringExtension.class)
public abstract class JpaTestBase extends TransactionTestBase {
  @Inject
  protected EntityManager entityManager;
}
