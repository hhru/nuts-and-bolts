package ru.hh.nab.metrics;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.hh.nab.metrics.TestUtils.tagsOf;

public class UniformHistogramsTest {

  private final UniformHistograms histograms = new UniformHistograms(100, 100);

  @Test
  public void oneThread() {
    Map<Tags, long[]> tagsToHistogram = histograms.getTagsToHistogramAndReset();
    assertTrue(tagsToHistogram.isEmpty());

    histograms.save(7, new Tag("label", "first"));
    tagsToHistogram = histograms.getTagsToHistogramAndReset();
    assertEquals(1, tagsToHistogram.size());
    assertArrayEquals(new long[] {7}, tagsToHistogram.get(new Tag("label", "first")));

    histograms.save(7, new Tag("label", "first"));
    histograms.save(7, new Tag("label", "first"));
    tagsToHistogram = histograms.getTagsToHistogramAndReset();
    assertEquals(1, tagsToHistogram.size());
    assertArrayEquals(new long[] {7, 7}, tagsToHistogram.get(new Tag("label", "first")));

    histograms.save(7, new Tag("label", "first"));
    histograms.save(7, new Tag("label", "second"));
    histograms.save(7, new Tag("label", "first"));
    tagsToHistogram = histograms.getTagsToHistogramAndReset();
    assertEquals(2, tagsToHistogram.size());
    assertArrayEquals(new long[] {7, 7}, tagsToHistogram.get(new Tag("label", "first")));
    assertArrayEquals(new long[] {7}, tagsToHistogram.get(new Tag("label", "second")));

    histograms.save(7, new Tag("label1", "first"), new Tag("label2", "second"));
    histograms.save(7, new Tag("label1", "second"), new Tag("label2", "first"));
    histograms.save(7, new Tag("label2", "second"), new Tag("label1", "first"));
    tagsToHistogram = histograms.getTagsToHistogramAndReset();
    assertArrayEquals(
      new long[] {7, 7},
      tagsToHistogram.get(tagsOf(new Tag("label1", "first"), new Tag("label2", "second")))
    );
    assertArrayEquals(
      new long[] {7},
      tagsToHistogram.get(tagsOf(new Tag("label1", "second"), new Tag("label2", "first")))
    );
  }

  @Test
  public void overflow() {
    UniformHistograms histograms = new UniformHistograms(100, 1);
    histograms.save(7, new Tag("label", "first"));
    histograms.save(13, new Tag("label", "second"));

    Map<Tags, long[]> tagsToHistogram = histograms.getTagsToHistogramAndReset();

    assertEquals(1, tagsToHistogram.size());
    assertArrayEquals(new long[] {7}, tagsToHistogram.get(new Tag("label", "first")));
  }
}
