package ru.hh.nab.health.limits;

public interface LeakListener {
  void leakDetected(LeaseToken token);
}
