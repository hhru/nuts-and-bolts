package ru.hh.nab.health.monitoring;

import org.junit.Assert;
import org.junit.Test;

public class LongHistogramToQuantilesTest {
  @Test
  public void simpleTest() {
    // 3 4 5 6 6 7 7 8 8 9 9 9
    long samples[] = {0, 0, 0, 1, 1, 1, 2, 2, 2, 3};
    long q[] = LongHistogramToQuantiles.quantiles(samples, 0.00, 0.5, 1.);
    Assert.assertEquals(3, q[0]);
    Assert.assertEquals(7, q[1]);
    Assert.assertEquals(9, q[2]);
  }
}
