package ru.hh.nab.health.monitoring;

import java.io.IOException;
import java.io.Writer;

public interface Dumpable {
  void dumpAndReset(Appendable out) throws IOException;
}
