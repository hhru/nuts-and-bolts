package ru.hh.nab.health.monitoring;

public interface CountingHistogram<T> {
  public void put(T value, long amount);
  public long[] getAndReset();
}
