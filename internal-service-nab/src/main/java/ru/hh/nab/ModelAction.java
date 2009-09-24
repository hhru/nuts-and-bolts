package ru.hh.nab;

import javax.persistence.EntityManager;

public interface ModelAction<T> {
  T perform(EntityManager store);
}