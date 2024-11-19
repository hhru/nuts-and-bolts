package ru.hh.nab.testbase.hibernate;

import jakarta.inject.Inject;
import org.hibernate.Session;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.hh.nab.testbase.extensions.SpringExtensionWithFailFast;
import ru.hh.nab.testbase.transaction.TransactionTestBase;

@ExtendWith(SpringExtensionWithFailFast.class)
public abstract class HibernateTestBase extends TransactionTestBase {
  @Inject
  protected Session session;
}
