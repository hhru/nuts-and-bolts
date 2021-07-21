package ru.hh.nab.metrics;

import static java.lang.System.currentTimeMillis;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static ru.hh.nab.metrics.TestUtils.tagsOf;

public class LongCountersTest {
  private final LongCounters counters = new LongCounters(300);

  @Test
  public void empty() {
    assertTrue(counters.getSnapshotAndReset().isEmpty());
  }

  @Test
  public void oneCounter() {
    Tag tag = new Tag("label", "first");
    counters.add(5, tag);
    Map<Tags, Long> tagsToValue;

    tagsToValue = counters.getSnapshotAndReset();
    assertEquals(1, tagsToValue.size());
    assertEquals(5, tagsToValue.get(tag).intValue());

    tagsToValue = counters.getSnapshotAndReset();
    assertEquals(1, tagsToValue.size());
    assertEquals(0,  tagsToValue.get(tag).intValue());

    tagsToValue = counters.getSnapshotAndReset();
    assertTrue(tagsToValue.isEmpty());
  }

  @Test
  public void differentTagsOrder() {
    counters.add(5, new Tag("label", "first"), new Tag("notlabel", "a"));
    counters.add(3, new Tag("notlabel", "a"), new Tag("label", "first"));

    Map<Tags, Long> tagsToCount = counters.getSnapshotAndReset();

    assertEquals(1, tagsToCount.size());
    assertEquals(8, tagsToCount.get(tagsOf(new Tag("label", "first"), new Tag("notlabel", "a"))).intValue());
  }

  @Test
  public void fourCounters() {
    counters.add(5, new Tag("label", "first"), new Tag("region", "vacancy"));
    counters.add(2, new Tag("label", "first"), new Tag("region", "resume"));
    counters.add(6, new Tag("label", "second"), new Tag("region", "resume"));
    counters.add(11, new Tag("label", "third"), new Tag("region", "resume"));

    Map<Tags, Long> tagsToCount = counters.getSnapshotAndReset();

    assertEquals(4, tagsToCount.size());
    assertEquals(5, tagsToCount.get(tagsOf(new Tag("label", "first"), new Tag("region", "vacancy"))).intValue());
    assertEquals(2, tagsToCount.get(tagsOf(new Tag("label", "first"), new Tag("region", "resume"))).intValue());
    assertEquals(6, tagsToCount.get(tagsOf(new Tag("label", "second"), new Tag("region", "resume"))).intValue());
    assertEquals(11, tagsToCount.get(tagsOf(new Tag("label", "third"), new Tag("region", "resume"))).intValue());
  }

  @Test
  public void twoThreads() throws InterruptedException {
    int increases = 1_000_000;
    Tag tag = new Tag("label", "first");
    Runnable increaseMetricTask = () -> {
      for (int i = 0; i < increases; i++) {
        counters.add(1, tag);
      }
    };

    int tests = 100;
    for (int t = 1; t <= tests; t++) {
      long start = currentTimeMillis();
      List<Map<Tags, Long>> snapshots = new ArrayList<>();

      Thread increaseMetricThread = new Thread(increaseMetricTask);
      increaseMetricThread.start();

      for (int i = 0; i < increases; i++) {
        counters.add(1, tag);
        if (i % 1000 == 0) {
          snapshots.add(counters.getSnapshotAndReset());
        }
      }

      increaseMetricThread.join();
      snapshots.add(counters.getSnapshotAndReset());

      int sum = 0;
      for (Map<Tags, Long> snapshot : snapshots) {
        Long snapshotValue = snapshot.get(tag);
        if (snapshotValue != null) {
          sum += snapshotValue;
        }
      }

      assertEquals(increases * 2, sum);

      System.out.println("finished iteration " + t + " out of " + tests + " in " + (currentTimeMillis() - start) + " ms");
    }
  }
}
