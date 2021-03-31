package ru.hh.nab.metrics;

import org.junit.jupiter.api.Test;

import static java.lang.System.currentTimeMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UniformHistogramTest {

  private final UniformHistogram histogram = new UniformHistogram(5_000);

  @Test
  public void twoThreads() throws InterruptedException {

    int increases = 1_000;
    Runnable task = () -> {
      for (int i = 0; i < increases; i++) {
        histogram.save(1);
        histogram.save(2);
      }
    };

    int tests = 100;
    for (int t = 1; t <= tests; t++) {
      long start = currentTimeMillis();

      Thread thread = new Thread(task);
      thread.start();

      for (int i = 0; i < increases; i++) {
        histogram.save(2);
        histogram.save(1);
      }

      thread.join();

      int firstSum = 0;
      int secondSum = 0;
      for (long value : histogram.getValuesAndReset()) {
        if (value == 1) {
          firstSum++;
        } else if (value == 2) {
          secondSum++;
        }
      }

      assertEquals(increases * 2, firstSum);
      assertEquals(increases * 2, secondSum);

      System.out.println("finished iteration " + t + " out of " + tests + " in " + (currentTimeMillis() - start) + " ms");
    }
  }

  @Test
  public void overflow() {
    UniformHistogram histogram = new UniformHistogram(1);
    histogram.save(7);
    histogram.save(13);

    long[] values = histogram.getValuesAndReset();

    assertEquals(1, values.length);
  }
}
