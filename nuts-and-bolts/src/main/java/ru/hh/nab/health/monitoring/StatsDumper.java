package ru.hh.nab.health.monitoring;

import com.google.common.collect.Maps;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatsDumper implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(StatsDumper.class);
  private final Map<Dumpable, Logger> dumpables;

  public StatsDumper(Map<String, Dumpable> dumpables) {
    this.dumpables = Maps.newHashMap();
    for (Map.Entry<String, Dumpable> d : dumpables.entrySet()) {
      this.dumpables.put(d.getValue(),
              LoggerFactory.getLogger(this.getClass().getName() + "." + d.getKey()));
    }
  }

  public void dump() throws IOException {
    for (Map.Entry<Dumpable, Logger> d: dumpables.entrySet()) {
      StringWriter out = new StringWriter();
      d.getKey().dumpAndReset(out);
      out.close();
      d.getValue().info(out.toString());
    }
  }

  @Override
  public void run() {
    try {
      dump();
    } catch (IOException e) {
      LOG.error("Error while dumping", e);
    }
  }
}
