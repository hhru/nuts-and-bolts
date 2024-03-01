package ru.hh.nab.metrics;

import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class CompactHistogramTest {

  @Test
  public void testSave() {
    CompactHistogram histogram = new CompactHistogram(128, 10);
    // 0 -> 0
    histogram.save(0);
    // 4 -> 8
    histogram.save(4);
    // 6 -> 8
    histogram.save(6);
    // 7 -> 8
    histogram.save(7);
    // 8 -> 8
    histogram.save(8);
    // 100 -> 104
    histogram.save(100);

    Map<Integer, Integer> values = histogram.getValueToCountAndReset();
    assertEquals(3, values.size());
    assertEquals(values.get(0), 1);
    assertEquals(values.get(8), 4);
    assertEquals(values.get(104), 1);
  }

  @Test
  public void testSaveWithCompactionRatio1() {
    CompactHistogram histogram = new CompactHistogram(1024, 1);
    int valuesCount = 1024;
    for (int value = 0; value < valuesCount; value++) {
      histogram.save(value);
    }

    Map<Integer, Integer> values = histogram.getValueToCountAndReset();
    assertEquals(valuesCount, values.size());
    for (int value = 0; value < valuesCount; value++) {
      assertEquals(1, values.get(value));
    }
  }
}
