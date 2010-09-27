package ru.hh.nab;

import javax.persistence.EntityManager;

public interface ModelCheckedAction<T, E extends Throwable> {
  T perform(EntityManager store) throws E;
}
