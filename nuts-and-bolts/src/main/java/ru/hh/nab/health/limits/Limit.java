package ru.hh.nab.health.limits;

public interface Limit {
  boolean acquire();
  void release();
}
