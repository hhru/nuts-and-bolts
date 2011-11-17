package ru.hh.nab.health.monitoring;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimingsLogger {
  private final static Logger PROBE = LoggerFactory.getLogger(TimingsLogger.class);

  private final Map<String, Long> probeDelays;
  private final long totalTimeThreshold;
  private final String timingsContext;
  private final List<LogRecord> logRecords = newArrayList();

  private volatile int timedAreasCount;
  private volatile boolean errorState;
  private volatile long startTime;

  public TimingsLogger(String context, Map<String, Long> probeDelays, long totalTimeThreshold) {
    this.timingsContext = context;
    this.probeDelays = probeDelays;
    this.totalTimeThreshold = totalTimeThreshold;
  }

  public synchronized void enterTimedArea() {
    if (startTime == 0)
      startTime = DateTimeUtils.currentTimeMillis();
    timedAreasCount++;
  }

  public synchronized void leaveTimedArea() {
    checkState(timedAreasCount >= 1);
    timedAreasCount--;
    if (timedAreasCount == 0)
      outputLoggedTimings();
  }

  public synchronized void setErrorState() {
    errorState = true;
  }

  private void outputLoggedTimings() {
    long timeSpent = DateTimeUtils.currentTimeMillis() - startTime;
    String timeTakenMsg = timingsContext + " : Time taken " + timeSpent + " ms";
    if (timeSpent < totalTimeThreshold && !errorState) {
      PROBE.debug(timeTakenMsg);
    } else {
      StringBuilder sb = new StringBuilder(timeTakenMsg);
      for (int idx = 0; idx < logRecords.size(); idx++) {
        long recordedTime = logRecords.get(idx).timestamp;
        long diffFromStart = recordedTime - startTime;
        long diffFromPrev = diffFromStart;
        if (idx > 0)
          diffFromPrev = recordedTime - logRecords.get(idx - 1).timestamp;
        sb.append("\n")
            .append(timingsContext).append(" : ")
            .append(diffFromStart).append("ms : ")
            .append(diffFromPrev).append("ms : ")
            .append(logRecords.get(idx).message);
      }
      if (errorState) {
        PROBE.error(sb.toString());
      } else {
        PROBE.warn(sb.toString());
      }
    }
  }

  public void probe(String event) {
    Long delay = probeDelays.get(event);
    if (delay == null) {
      addLogRecord(event);
    } else {
      addLogRecord(format("Pausing before event %s for %d millis", event, delay));
      sleep(delay);
    }
  }

  private void addLogRecord(String message) {
    LogRecord logRecord = new LogRecord(message, DateTimeUtils.currentTimeMillis());
    logRecords.add(logRecord);
  }
  
  private static void sleep(long delay) {
    try {
      Thread.sleep(delay);
    } catch (InterruptedException e) {
      PROBE.warn("Interrupted", e);
      Thread.currentThread().interrupt();
    }
  }

  private static class LogRecord {
    public final Long timestamp;
    public final String message;

    private LogRecord(String message, Long timestamp) {
      this.message = message;
      this.timestamp = timestamp;
    }
  }
}
