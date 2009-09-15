package ru.hh.nab;

import org.hibernate.Session;

public interface HibernateCheckedAction<T, E extends Throwable> {
  T perform(Session s) throws E;
}
