package ru.hh.nab.kafka.util;

import java.util.function.Supplier;

public final class SleepUtils {

  private static final long SMALL_INTERVAL_THRESHOLD_MS = 500;
  private static final int DEFAULT_SLEEP_INTERVAL_MS = 100;
  private static final int SMALL_SLEEP_INTERVAL_MS = 10;

  public static void stoppableSleep(long intervalMs, Supplier<Boolean> sleepCondition) throws InterruptedException {
    long timeout = System.currentTimeMillis() + intervalMs;
    long sleepInterval = intervalMs > SMALL_INTERVAL_THRESHOLD_MS ? DEFAULT_SLEEP_INTERVAL_MS : Math.min(intervalMs, SMALL_SLEEP_INTERVAL_MS);
    while (System.currentTimeMillis() < timeout && sleepCondition.get()) {
      //noinspection BusyWait
      Thread.sleep(sleepInterval);
    }
  }

  private SleepUtils() {
  }
}
