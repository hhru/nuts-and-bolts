package ru.hh.nab.common.executor;

import java.time.Duration;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.common.util.ThreadDumpWriter;

public class ThreadDiagnosticRejectedExecutionHandler implements RejectedExecutionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ThreadDiagnosticRejectedExecutionHandler.class);
  private static final int THREAD_DUMP_PRINTING_MIN_DELAY_IN_MINUTES = 5;
  private static final int THREAD_DUMP_LINES_IN_LOG_MESSAGE = 500;

  private static final ThreadDumpWriter THREAD_DUMP_WRITER = new ThreadDumpWriter("RejectedExecutionDiagnostic",
      THREAD_DUMP_LINES_IN_LOG_MESSAGE, Duration.ofMinutes(THREAD_DUMP_PRINTING_MIN_DELAY_IN_MINUTES).toMillis());

  @Override
  public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
    String threadPoolPhrase;
    if (executor instanceof MonitoredThreadPoolExecutor) {
      threadPoolPhrase = ((MonitoredThreadPoolExecutor) executor).getThreadPoolName() + " thread pool";
    } else {
      threadPoolPhrase = "Thread pool";
    }
    LOGGER.warn("{} is low on threads: size={}, activeCount={}, queueSize={}",
        threadPoolPhrase, executor.getPoolSize(), executor.getActiveCount(), executor.getQueue().size());

    THREAD_DUMP_WRITER.tryDumpThreads();

    throw new RejectedExecutionException(threadPoolPhrase + " is low on threads");
  }
}
