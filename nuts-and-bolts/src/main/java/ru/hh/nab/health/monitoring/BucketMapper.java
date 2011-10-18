package ru.hh.nab.health.monitoring;

public interface BucketMapper<T> {
  int bucket(T value);

  int max();
}
