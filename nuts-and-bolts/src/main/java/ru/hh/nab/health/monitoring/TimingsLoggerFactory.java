package ru.hh.nab.health.monitoring;

import com.google.common.base.Optional;
import java.util.Map;

public class TimingsLoggerFactory {

  public static final long DEFAULT_TIME_TOLERANCE = 15000; // 15 seconds

  // Event -> Delay
  private final Map<String, Long> probeDelays;
  private final long totalTimeThreshold;

  public TimingsLoggerFactory(Map<String, Long> probeDelays, Optional<Long> totalTimeThreshold) {
    this.probeDelays = probeDelays;
    this.totalTimeThreshold = (totalTimeThreshold.isPresent())
        ? totalTimeThreshold.get()
        : DEFAULT_TIME_TOLERANCE;
  }

  public TimingsLogger getLogger(String context) {
    return new TimingsLogger(context, probeDelays, totalTimeThreshold);
  }
}
