package ru.hh.nab.metrics;

import static java.lang.System.currentTimeMillis;
import java.util.ArrayList;
import java.util.Collection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MaxPerfTest {

  private static final int tests = 40;
  private static final int increases = 200_000_000;
  private static final int maxValue = 999;
  private static final int snapshotIteration = 100_000;

  public static void main(String[] args) throws InterruptedException {
    for(int t=1; t<=tests; t++) {
      test(t);
    }
  }

  private static void test(int testIteration) throws InterruptedException {
    Max max = new Max(0);

    Runnable task = () -> {
      for (int i=1; i<=increases; i++) {
        save(max, i);
      }
    };
    Thread thread = new Thread(task);

    Collection<Integer> snapshots = new ArrayList<>(increases / snapshotIteration);

    long start = currentTimeMillis();

    thread.start();
    for (int i = 1; i <= increases; i++) {
      save(max, i);
      if (i % snapshotIteration == 0) {
        snapshots.add(max.getAndReset());
      }
    }
    thread.join();
    System.out.println("Max " + testIteration + ' ' + (currentTimeMillis() - start) + " ms");
    snapshots.add(max.getAndReset());

    checkSnapshots(snapshots);
  }

  private static void save(Max max, int iteration) {
    max.save(iteration % (maxValue+1));
  }

  private static void checkSnapshots(Collection<Integer> snapshots) {
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
