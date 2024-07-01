package ru.hh.nab.testbase.hibernate;

import jakarta.inject.Inject;
import org.hibernate.Session;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.hh.nab.testbase.transaction.TransactionTestBase;

@ExtendWith(SpringExtension.class)
public abstract class HibernateTestBase extends TransactionTestBase {
  @Inject
  protected Session session;
}
