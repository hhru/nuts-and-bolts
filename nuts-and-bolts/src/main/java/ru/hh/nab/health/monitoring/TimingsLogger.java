package ru.hh.nab.health.monitoring;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.hh.util.LogLevel;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class TimingsLogger {
  private final static Logger LOG = LoggerFactory.getLogger(TimingsLogger.class);

  private final static String REQ_H_X_REQUEST_ID = "req.h.x-request-id";

  private final Map<String, Long> probeDelays;
  private final long totalTimeThreshold;
  private final String timingsContext;
  private final String requestId;
  private final List<LogRecord> logRecords = newArrayList();

  private volatile int timedAreasCount;
  private volatile boolean errorState;
  private volatile long startTime;

  public TimingsLogger(String context, String requestId, Map<String, Long> probeDelays, long totalTimeThreshold) {
    this.timingsContext = context;
    this.requestId = requestId;
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

  /** Will cause all probes to be logged as ERROR when the timed area is left. */
  public synchronized void setErrorState() {
    errorState = true;
  }

  public synchronized void probe(String event) {
    Long delay = probeDelays.get(event);
    if (delay == null) {
      addLogRecord(event);
    } else {
      addLogRecord(format("Pausing before event %s for %d millis", event, delay));
      sleep(delay);
    }
  }

  private static String probeMessage(long start, long prev, String name, int cols) {
    return String.format("  total(ms): %" + cols + "d %-" + (cols + 1) + "s %s%n", start, String.format("+%d", prev), name);
  }

  private void outputLoggedTimings() {
    final long endTime = DateTimeUtils.currentTimeMillis();
    long timeSpent = endTime - startTime;

    String copy = MDC.get(REQ_H_X_REQUEST_ID);
    if (requestId != null)
      MDC.put(REQ_H_X_REQUEST_ID, requestId);
    else if (copy != null)
      MDC.remove(REQ_H_X_REQUEST_ID);

    try { // clear MDC in finally

      final boolean exceeded = timeSpent > totalTimeThreshold;
      final boolean logProbes = errorState || exceeded;

      StringBuilder totalTimeBuilder = new StringBuilder();
      if (StringUtils.isNotBlank(timingsContext)) {
        totalTimeBuilder.append("Context : ").append(timingsContext).append(" ; ");
      }
      if (exceeded) {
        totalTimeBuilder.append(totalTimeThreshold);
        totalTimeBuilder.append("ms tolerance exceeded, ");
      }
      totalTimeBuilder.append("total time ");
      totalTimeBuilder.append(timeSpent);
      totalTimeBuilder.append(" ms");
      final String totalTimeMessage = totalTimeBuilder.toString();
      if (!logProbes) {
        LOG.debug(totalTimeMessage);
        return;
      }
      LogLevel.Level logLevel = errorState ? LogLevel.Level.ERROR :  LogLevel.Level.WARN;
      LogLevel.log(LOG, logLevel, totalTimeMessage);

      final int totalRecords = logRecords.size();
      int maxCols = 0;
      if (totalRecords > 0) {
        maxCols = Long.toString(endTime - startTime).length();
      }
      for (int idx = 0; idx < totalRecords; idx++) {
        long recordTimestamp = logRecords.get(idx).timestamp;
        long diffFromStart = recordTimestamp - startTime;
        long diffFromPrev = diffFromStart;
        if (idx > 0)
          diffFromPrev = recordTimestamp - logRecords.get(idx - 1).timestamp;

        LogLevel.log(LOG, logLevel, probeMessage(diffFromStart, diffFromPrev, logRecords.get(idx).message, maxCols));

        if (idx == totalRecords - 1) {
          LogLevel.log(LOG, logLevel, probeMessage(endTime - startTime, endTime - recordTimestamp, "<timed area end>", maxCols));
        }
      }
    } finally {
      if (copy != null)
        MDC.put(REQ_H_X_REQUEST_ID, copy);
      else if (requestId != null)
        MDC.remove(REQ_H_X_REQUEST_ID);
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
      LOG.warn("Interrupted", e);
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
