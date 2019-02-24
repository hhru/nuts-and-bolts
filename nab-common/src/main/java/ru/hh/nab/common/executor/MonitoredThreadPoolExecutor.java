package ru.hh.nab.common.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.metrics.Max;
import ru.hh.metrics.StatsDSender;
import ru.hh.nab.common.properties.FileSettings;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.Executors.defaultThreadFactory;

public class MonitoredThreadPoolExecutor implements Executor {
  private final static Logger LOGGER = LoggerFactory.getLogger(MonitoredThreadPoolExecutor.class);

  private final ThreadPoolExecutor delegate;
  private final String threadPoolName;
  private final Max poolSize = new Max(0);
  private final Max activeCount = new Max(0);
  private final Max queueSize = new Max(0);

  public MonitoredThreadPoolExecutor(String threadPoolName, String serviceName, FileSettings threadPoolSettings, StatsDSender statsDSender) {
    this.threadPoolName = threadPoolName;
    this.delegate = createThreadPoolExecutor(threadPoolSettings);

    statsDSender.sendMaxPeriodically(getFullMetricName(serviceName, "size"), poolSize);
    statsDSender.sendMaxPeriodically(getFullMetricName(serviceName, "activeCount"), activeCount);
    statsDSender.sendMaxPeriodically(getFullMetricName(serviceName, "queueSize"), queueSize);
  }

  @Override
  public void execute(Runnable command) {
    poolSize.save(delegate.getPoolSize());
    activeCount.save(delegate.getActiveCount());
    queueSize.save(delegate.getQueue().size());

    delegate.execute(command);
  }

  private ThreadPoolExecutor createThreadPoolExecutor(FileSettings threadPoolSettings) {
    var defaultThreadFactory = defaultThreadFactory();

    int minThreads = ofNullable(threadPoolSettings.getInteger("minSize")).orElse(8);
    int maxThreads = ofNullable(threadPoolSettings.getInteger("maxSize")).orElse(16);
    int queueSize = ofNullable(threadPoolSettings.getInteger("queueSize")).orElse(16);
    int keepAliveTimeSec = ofNullable(threadPoolSettings.getInteger("keepAliveTimeSec")).orElse(60);

    var count = new AtomicLong(0);
    ThreadFactory threadFactory = r -> {
      Thread thread = defaultThreadFactory.newThread(r);
      thread.setName(String.format("%s-monitored-pool-thread-%s", threadPoolName, count.getAndIncrement()));
      thread.setDaemon(true);
      return thread;
    };

    var threadPoolExecutor = new ThreadPoolExecutor(
      minThreads, maxThreads, keepAliveTimeSec, TimeUnit.SECONDS, new ArrayBlockingQueue<>(queueSize), threadFactory,
      (r, executor) -> {
        LOGGER.warn("{} thread pool is low on threads: size={}, activeCount={}, queueSize={}",
          threadPoolName, executor.getPoolSize(), executor.getActiveCount(), executor.getQueue().size());
        throw new RejectedExecutionException(threadPoolName + " thread pool is low on threads");
      });

    threadPoolExecutor.prestartAllCoreThreads();
    return threadPoolExecutor;
  }

  private String getFullMetricName(String serviceName, String shortMetricName) {
    return serviceName + '.' + threadPoolName + ".threadPool." + shortMetricName;
  }
}
