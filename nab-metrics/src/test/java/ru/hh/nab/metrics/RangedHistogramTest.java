package ru.hh.nab.metrics;

import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class RangedHistogramTest {

  @Test
  public void testSave() {
    RangedHistogram histogram = new RangedHistogram(128);
    // 0 -> 0
    histogram.save(0);
    // 4 -> 4
    histogram.save(4);
    // 6 -> 8
    histogram.save(6);
    // 7 -> 8
    histogram.save(7);
    // 8 -> 8
    histogram.save(8);
    // 100 -> 128
    histogram.save(100);

    Map<Integer, Integer> values = histogram.getValueToCountAndReset();
    assertEquals(4, values.size());
    assertEquals(values.get(0), 1);
    assertEquals(values.get(4), 1);
    assertEquals(values.get(8), 3);
    assertEquals(values.get(128), 1);
  }
}
