package ru.hh.nab.health.monitoring;

import com.google.common.base.Function;

public class LongHistogramToQuantiles implements Function<long[], long[]> {
  private double[] q;

  public LongHistogramToQuantiles(double... q) {
    this.q = q;
  }

  @Override
  public long[] apply(long[] samples) {
    return quantiles(samples, q);
  }

  public double[] percentiles() {
    return q;
  }

  public static long[] quantiles(long[] samples, double... q) {
    long sum = 0;
    for (long f : samples) {
      sum += f;
    }

    long[] ret = new long[q.length];

    for (int i = 0; i < q.length; i++) {
      double expectedSum = sum * q[i];
      long runningSum = 0;
      int j;
      for (j = 0; j < samples.length; j++) {
        runningSum += samples[j];
        if (expectedSum < runningSum) {
          ret[i] = j;
          break;
        }
      }
      if (j == samples.length) {
        ret[i] = j-1;
      }
    }
    return ret;
  }
}
