package ru.hh.nab.metrics.executor;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadDiagnosticRejectedExecutionHandler implements RejectedExecutionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ThreadDiagnosticRejectedExecutionHandler.class);
  private static final int THREAD_DUMP_PRINTING_MIN_DELAY_IN_MINUTES = 5;
  private static final int THREAD_DUMP_LINES_IN_LOG_MESSAGE = 500;

  private final ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<>(1);
  private volatile String threadNamePrefix;

  public ThreadDiagnosticRejectedExecutionHandler() {
    Pattern logLinesPattern = Pattern.compile("(?:[^\\n]+\\n+){" + (THREAD_DUMP_LINES_IN_LOG_MESSAGE - 1) + "}[^\\n]+");
    new Thread(() -> {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          queue.take();
          writePoolThreadDump(logLinesPattern);
          Thread.sleep(Duration.ofMinutes(THREAD_DUMP_PRINTING_MIN_DELAY_IN_MINUTES).toMillis());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          LOGGER.warn("ThreadDiagnosticRejectedExecutionHandler was interrupted", e);
          break;
        }
      }
    }, "ThreadDumpWriter-RejectedExecutionDiagnostic").start();
  }

  @Override
  public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
    String threadPoolPhrase;
    if (executor instanceof MonitoredThreadPoolExecutor m) {
      threadPoolPhrase = m.getThreadPoolName() + " thread pool";
      threadNamePrefix = m.getThreadPoolName() + "-";
    } else {
      threadPoolPhrase = "Thread pool";
      threadNamePrefix = null;
    }
    LOGGER.warn("{} is low on threads: size={}, activeCount={}, queueSize={}",
        threadPoolPhrase, executor.getPoolSize(), executor.getActiveCount(), executor.getQueue().size());

    queue.offer(1);

    throw new RejectedExecutionException(threadPoolPhrase + " is low on threads");
  }

  private void writePoolThreadDump(Pattern logLinesPattern) {
    ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    String prefix = threadNamePrefix;

    StringBuilder threadDump = new StringBuilder(System.lineSeparator());
    for (ThreadInfo threadInfo : threadMXBean.dumpAllThreads(true, true)) {
      if (prefix == null || threadInfo.getThreadName().startsWith(prefix)) {
        threadDump.append(threadInfo.toString());
      }
    }
    String threadDumpString = "Thread pool dump:\n" + threadDump;
    Matcher matcher = logLinesPattern.matcher(threadDumpString);
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
