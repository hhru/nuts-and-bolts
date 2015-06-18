package ru.hh.nab.health.monitoring;

import java.io.IOException;

public interface Dumpable {
  void dumpAndReset(Appendable out) throws IOException;
}
