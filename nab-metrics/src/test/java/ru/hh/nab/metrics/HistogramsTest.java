package ru.hh.nab.metrics;

import static java.lang.System.currentTimeMillis;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static ru.hh.nab.metrics.TestUtils.tagsOf;

public class HistogramsTest {

  private final Histograms histograms = new Histograms(100, 100);

  @Test
  public void oneThread() {
    Map<Tags, Map<Integer, Integer>> tagsToHistogram = histograms.getTagsToHistogramAndReset();
    assertTrue(tagsToHistogram.isEmpty());

    histograms.save(7, new Tag("label", "first"));
    tagsToHistogram = histograms.getTagsToHistogramAndReset();
    assertEquals(1, tagsToHistogram.size());
    assertEquals(1, tagsToHistogram.get(new Tag("label", "first")).get(7).intValue());

    histograms.save(7, new Tag("label", "first"));
    histograms.save(7, new Tag("label", "first"));
    tagsToHistogram = histograms.getTagsToHistogramAndReset();
    assertEquals(1, tagsToHistogram.size());
    assertEquals(2, tagsToHistogram.get(new Tag("label", "first")).get(7).intValue());

    histograms.save(7, new Tag("label", "first"));
    histograms.save(7, new Tag("label", "second"));
    histograms.save(7, new Tag("label", "first"));
    tagsToHistogram = histograms.getTagsToHistogramAndReset();
    assertEquals(2, tagsToHistogram.size());
    assertEquals(2, tagsToHistogram.get(new Tag("label", "first")).get(7).intValue());
    assertEquals(1, tagsToHistogram.get(new Tag("label", "second")).get(7).intValue());

    histograms.save(7, new Tag("label1", "first"), new Tag("label2", "second"));
    histograms.save(7, new Tag("label1", "second"), new Tag("label2", "first"));
    histograms.save(7, new Tag("label2", "second"), new Tag("label1", "first"));
    tagsToHistogram = histograms.getTagsToHistogramAndReset();
    assertEquals(
      2,
      tagsToHistogram.get(tagsOf(new Tag("label1", "first"), new Tag("label2", "second")))
        .get(7).intValue()
    );
    assertEquals(
      1,
      tagsToHistogram.get(tagsOf(new Tag("label1", "second"), new Tag("label2", "first")))
        .get(7).intValue()
    );
  }

  @Test
  public void twoThreads() throws InterruptedException {

    int increases = 100_000;
    Tag tag1 = new Tag("label", "first");
    Tag tag2 = new Tag("label", "second");
    Runnable task = () -> {
      for (int i = 0; i < increases; i++) {
        histograms.save(1, tag1);
        histograms.save(1, tag2);
      }
    };

    int tests = 100;
    for (int t = 1; t <= tests; t++) {
      long start = currentTimeMillis();
      List<Map<Tags, Map<Integer, Integer>>> snapshots = new ArrayList<>();

      Thread thread = new Thread(task);
      thread.start();

      for (int i = 0; i < increases; i++) {
        histograms.save(1, tag2);
        histograms.save(1, tag1);
        if (i % 1000 == 0) {
          snapshots.add(histograms.getTagsToHistogramAndReset());
        }
      }

      thread.join();
      snapshots.add(histograms.getTagsToHistogramAndReset());

      int firstSum = 0;
      int secondSum = 0;
      for (Map<Tags, Map<Integer, Integer>> snapshot : snapshots) {
        Integer firstCount = snapshot.get(tag1).get(1);
        if (firstCount != null) {
          firstSum += firstCount;
        }
        Integer secondCount = snapshot.get(tag2).get(1);
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
    Histograms histograms = new Histograms(100, 1);
    histograms.save(7, new Tag("label", "first"));
    histograms.save(13, new Tag("label", "second"));

    Map<Tags, Map<Integer, Integer>> tagsToHistogram = histograms.getTagsToHistogramAndReset();

    assertEquals(1, tagsToHistogram.size());
    assertEquals(1, tagsToHistogram.get(new Tag("label", "first")).get(7).intValue());
  }
}
