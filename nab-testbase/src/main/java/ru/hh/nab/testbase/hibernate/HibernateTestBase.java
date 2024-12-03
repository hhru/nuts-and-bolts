package ru.hh.nab.testbase.hibernate;

import jakarta.inject.Inject;
import org.hibernate.Session;
import ru.hh.nab.testbase.transaction.TransactionTestBase;

public abstract class HibernateTestBase extends TransactionTestBase {
  @Inject
  protected Session session;
}
