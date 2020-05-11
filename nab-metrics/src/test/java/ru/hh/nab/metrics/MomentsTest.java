package ru.hh.nab.metrics;

import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class MomentsTest {
  private static final int THREAD_COUNT = 1000;
  private static final float DELTA = 0.00001f;
  private static final float[] SAMPLE = new float[] {10.0454f, 20.5545f, 30.4444f, 40.6656f, 15.435354f, 11.456233f, 25.56565f};

  @Test
  public void testUpdate() {
    Moments moments = new Moments();

    for (float value : SAMPLE) {
      moments.update(value);
    }

    Moments.MomentsData data = moments.getAndReset();

    assertEquals(7, data.getNumber());
    assertEquals(10.0454f, data.getMin(), DELTA);
    assertEquals(40.6656f, data.getMax(), DELTA);
    assertEquals(22.0238770076f, data.getMean(), DELTA);
    assertEquals(104.527133008f, data.getVariance(), DELTA);
  }

  @Test
  public void testMerge() {
    Moments moments = new Moments();

    for (int i = 0; i < SAMPLE.length; i += 2) {
      Moments partial = new Moments();
      partial.update(SAMPLE[i]);
      if (i + 1 != SAMPLE.length) {
        partial.update(SAMPLE[i + 1]);
      }

      moments.merge(partial);
    }

    Moments.MomentsData data = moments.getAndReset();

    assertEquals(7, data.getNumber());
    assertEquals(10.0454f, data.getMin(), DELTA);
    assertEquals(40.6656f, data.getMax(), DELTA);
    assertEquals(22.0238770076f, data.getMean(), DELTA);
    assertEquals(104.527133008f, data.getVariance(), DELTA);
  }

  @Test
  public void multipleThreads() throws InterruptedException {
    Moments moments = new Moments();
    List<Thread> threads = new ArrayList<>(THREAD_COUNT);

    for (int i =0; i < THREAD_COUNT; i++) {
      Thread thread = new Thread(() -> {
        Moments currentMoments = new Moments();

        for (float value : SAMPLE) {
          currentMoments.update(value);
        }

        moments.merge(currentMoments);
      });

      threads.add(thread);
    }

    threads.forEach(Thread::start);

    for (Thread thread : threads) {
      thread.join();
    }

    moments.merge(new Moments());

    Moments.MomentsData data = moments.getAndReset();

    assertEquals(7 * THREAD_COUNT, data.getNumber());
    assertEquals(10.0454f, data.getMin(), DELTA);
    assertEquals(40.6656f, data.getMax(), DELTA);
    assertEquals(22.0238770076f, data.getMean(), DELTA);
    assertEquals(104.527133008f, data.getVariance(), DELTA);
  }
}
