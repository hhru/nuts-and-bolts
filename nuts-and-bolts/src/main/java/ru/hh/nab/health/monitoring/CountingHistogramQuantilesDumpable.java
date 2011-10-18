package ru.hh.nab.health.monitoring;

import java.io.IOException;

public class CountingHistogramQuantilesDumpable<T> implements Dumpable {
  private final CountingHistogram<T> histo;
  private final LongHistogramToQuantiles quantiles;

  public CountingHistogramQuantilesDumpable(CountingHistogram<T> histo, double... q) {
    this.histo = histo;
    this.quantiles = new LongHistogramToQuantiles(q);
  }

  @Override
  public void dumpAndReset(Appendable out) throws IOException {
    long[] q = quantiles.apply(histo.getAndReset());
    for (int i = 0; i < q.length; i++) {
      out.append(String.format("%.02f: %d\n", quantiles.percentiles()[i], q[i]));
    }
  }
}
