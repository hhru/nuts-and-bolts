package ru.hh.nab.metrics;

import static java.lang.System.currentTimeMillis;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class HistogramTest {

  private final Histogram histogram = new SimpleHistogram(100);

  @Test
  public void oneThread() {
    Map<Integer, Integer> valueToCount = histogram.getValueToCountAndReset();
    assertTrue(valueToCount.isEmpty());

    histogram.save(7);
    valueToCount = histogram.getValueToCountAndReset();
    assertEquals(1, valueToCount.size());
    assertEquals(1, valueToCount.get(7).intValue());

    histogram.save(7);
    histogram.save(7);
    valueToCount = histogram.getValueToCountAndReset();
    assertEquals(1, valueToCount.size());
    assertEquals(2, valueToCount.get(7).intValue());

    histogram.save(7);
    histogram.save(13);
    histogram.save(7);
    valueToCount = histogram.getValueToCountAndReset();
    assertEquals(2, valueToCount.size());
    assertEquals(2, valueToCount.get(7).intValue());
    assertEquals(1, valueToCount.get(13).intValue());
  }

  @Test
  public void twoThreads() throws InterruptedException {

    int increases = 1_000_000;
    Runnable task = () -> {
      for (int i = 0; i < increases; i++) {
        histogram.save(1);
        histogram.save(2);
      }
    };

    int tests = 100;
    for (int t = 1; t <= tests; t++) {
      long start = currentTimeMillis();
      List<Map<Integer, Integer>> snapshots = new ArrayList<>();

      Thread thread = new Thread(task);
      thread.start();

      for (int i = 0; i < increases; i++) {
        histogram.save(2);
        histogram.save(1);
        if (i % 1000 == 0) {
          snapshots.add(histogram.getValueToCountAndReset());
        }
      }

      thread.join();
      snapshots.add(histogram.getValueToCountAndReset());

      int firstSum = 0;
      int secondSum = 0;
      for (Map<Integer, Integer> snapshot : snapshots) {
        Integer firstCount = snapshot.get(1);
        if (firstCount != null) {
          firstSum += firstCount;
        }
        Integer secondCount = snapshot.get(2);
        if (secondCount != null) {
          secondSum += secondCount;
        }
      }

      assertEquals(increases * 2, firstSum);
      assertEquals(increases * 2, secondSum);

      System.out.println("finished iteration " + t + " out of " + tests + " in " + (currentTimeMillis() - start) + " ms");
    }
  }

  @Test
  public void overflow() {
    Histogram histogram = new SimpleHistogram(1);
    histogram.save(7);
    histogram.save(13);

    Map<Integer, Integer> valueToCount = histogram.getValueToCountAndReset();

    assertEquals(1, valueToCount.size());
    assertEquals(1, valueToCount.get(7).intValue());
  }

  @Test
  public void testCompactHistogram() {
    int histogramSize = 512;
    int compactionRatio = 32;
    CompactHistogram compactHistogram = new CompactHistogram(histogramSize, compactionRatio);
    IntStream.range(0, 10000).forEach(compactHistogram::save);

    Map<Integer, Integer> values = compactHistogram.getValueToCountAndReset();
    assertTrue(values.size() <= histogramSize);
    assertTrue(values.get(compactionRatio) > 1);
  }
}
