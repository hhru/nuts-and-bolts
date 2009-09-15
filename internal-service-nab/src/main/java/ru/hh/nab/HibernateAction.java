package ru.hh.nab;

import org.hibernate.Session;

public interface HibernateAction<T> {
  T perform(Session s);
}