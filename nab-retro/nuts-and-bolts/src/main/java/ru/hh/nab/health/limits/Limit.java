package ru.hh.nab.health.limits;

public interface Limit {
  LeaseToken acquire();
  int getMax();
  String getName();
}
