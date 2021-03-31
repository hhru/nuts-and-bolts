package ru.hh.nab.metrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.currentTimeMillis;

public class LongCountersPerfTest {

  private static final int tests = 40;
  private static final int increases = 100_000_000;
  private static final int snapshotIteration = 100_000;
  private static final String[] tagValues = createTagValues(5);

  private static String[] createTagValues(int numOfTagValues) {
    String[] tagValues = new String[numOfTagValues];
    for (int i=0; i<tagValues.length; i++) {
      tagValues[i] = Integer.toString(i);
    }
    return tagValues;
  }

  public static void main(String[] args) throws InterruptedException {
    for(int t=1; t<=tests; t++) {
      test(t);
    }
  }

  private static void test(int testIteration) throws InterruptedException {
    LongCounters counters = new LongCounters(300);

    Runnable addTask = () -> {
      for (int i = 1; i <= increases; i++) {
        add(counters, i);
      }
    };
    Thread increaseMetricThread = new Thread(addTask);

    Collection<Map<Tags, Long>> snapshots = new ArrayList<>(increases / snapshotIteration);

    long start = currentTimeMillis();

    increaseMetricThread.start();

    for (int i = 1; i <= increases; i++) {
      add(counters, i);
      if (i % snapshotIteration == 0) {
        snapshots.add(counters.getSnapshotAndReset());
      }
    }

    increaseMetricThread.join();

    System.out.println("Counters " + testIteration + " " + (currentTimeMillis() - start) + " ms");

    snapshots.add(counters.getSnapshotAndReset());
    checkSnapshots(snapshots);
  }

  private static void add(LongCounters counters, int iteration) {
    counters.add(1, createTag(iteration % tagValues.length));
  }

  private static Tag createTag(int tagValueIndex) {
    return new Tag("label", tagValues[tagValueIndex]);
  }

  private static void checkSnapshots(Collection<Map<Tags, Long>> snapshots) {
    Map<Tags, Long> tagsToValue = merge(snapshots);
    for (int i = 0; i<tagValues.length; i++) {
      long expected = increases * 2 / tagValues.length;
      long actual = tagsToValue.get(createTag(i));
      if (actual != expected) {
        throw new IllegalStateException("tag " + i + " expected " + expected + " got " + actual);
      }
    }
  }

  private static Map<Tags, Long> merge(Collection<Map<Tags, Long>> snapshots) {
    Map<Tags, Long> tagsToTotalValue = new HashMap<>();
    for (Map<Tags, Long> snapshot : snapshots) {
      for (Map.Entry<Tags, Long> tagsAndSnapshotValue : snapshot.entrySet()) {
        Long totalValue = tagsToTotalValue.get(tagsAndSnapshotValue.getKey());
        if (totalValue == null) {
          totalValue = 0L;
        }
        tagsToTotalValue.put(tagsAndSnapshotValue.getKey(), totalValue + tagsAndSnapshotValue.getValue());
      }
    }
    return tagsToTotalValue;
  }

}
