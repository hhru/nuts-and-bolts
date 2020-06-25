package ru.hh.nab.common.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadDumpWriter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ThreadDumpWriter.class);

  private final ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<>(1);

  public ThreadDumpWriter(String name, int linesInLogMessage, long delayMs) {
    Pattern logLinesPattern = Pattern.compile("(?:[^\\n]+\\n+){" + (linesInLogMessage - 1) + "}[^\\n]+");

    new Thread(() -> {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          queue.take();
          writeThreadDump(logLinesPattern);
          Thread.sleep(delayMs);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          LOGGER.warn("ThreadDumpWriter was interrupted", e);
          break;
        }
      }
    }, "ThreadDumpWriter-" + name).start();
  }

  public void tryDumpThreads() {
    queue.offer(1);
  }

  private static void writeThreadDump(Pattern logLinesPattern) {
    StringBuilder threadDump = new StringBuilder(System.lineSeparator());
    ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    for (ThreadInfo threadInfo : threadMXBean.dumpAllThreads(true, true)) {
      threadDump.append(threadInfo.toString());
    }
    String threadDumpString = "Thread dump:\n" + threadDump.toString();
    final Matcher matcher = logLinesPattern.matcher(threadDumpString);
    int tailIndex = 0;
    while (matcher.find()) {
      LOGGER.warn(matcher.group(0));
      tailIndex = matcher.end();
    }
    if (tailIndex < threadDumpString.length()) {
      LOGGER.warn(threadDumpString.substring(tailIndex));
    }
  }
}
