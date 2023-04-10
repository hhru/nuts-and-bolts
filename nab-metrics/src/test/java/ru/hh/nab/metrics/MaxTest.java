package ru.hh.nab.metrics;

import static java.lang.System.currentTimeMillis;
import java.util.ArrayList;
import java.util.Collection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class MaxTest {

  private final Max max = new Max(0);

  @Test
  public void oneThread() {
    assertEquals(0, max.getAndReset());

    max.save(1);
    assertEquals(1, max.getAndReset());
    assertEquals(0, max.getAndReset());

    max.save(2);
    max.save(1);
    assertEquals(2, max.getAndReset());
    assertEquals(0, max.getAndReset());
  }

  @Test
  public void twoThreads() throws InterruptedException {

    int increases = 1_000_000;
    int maxValue = 99;
    int snapshotIteration = 1000;
    Runnable task = () -> {
      for (int i = 0; i < increases; i++) {
        max.save(i % (maxValue + 1));
      }
    };

    int tests = 100;
    for (int t = 1; t <= tests; t++) {
      Collection<Integer> snapshots = new ArrayList<>(increases / snapshotIteration);

      Thread thread = new Thread(task);

      long start = currentTimeMillis();

      thread.start();
      for (int i = 0; i < increases; i++) {
        max.save(i % (maxValue + 1));
        if (i % 1000 == 0) {
          snapshots.add(max.getAndReset());
        }
      }
      thread.join();
      snapshots.add(max.getAndReset());
      System.out.println("finished iteration " + t + " out of " + tests + " in " + (currentTimeMillis() - start) + " ms");

      int maxOfSnapshots = 0;
      for (int snapshot : snapshots) {
        assertTrue(snapshot >= 0);
        assertTrue(snapshot <= maxValue);
        if (snapshot > maxOfSnapshots) {
          maxOfSnapshots = snapshot;
        }
      }
      assertEquals(maxValue, maxOfSnapshots);
    }

  }

}
