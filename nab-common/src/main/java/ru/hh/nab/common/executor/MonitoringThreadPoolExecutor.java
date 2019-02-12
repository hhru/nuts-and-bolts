package ru.hh.nab.common.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.metrics.StatsDSender;
import ru.hh.nab.common.properties.FileSettings;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Optional.ofNullable;

public class MonitoringThreadPoolExecutor implements Executor {
  private final static Logger LOGGER = LoggerFactory.getLogger(MonitoringThreadPoolExecutor.class);

  private final ThreadPoolExecutor delegate;
  private final String threadPoolName;

  public MonitoringThreadPoolExecutor(String threadPoolName, String serviceName, FileSettings threadPoolSettings, StatsDSender statsDSender) {
    this.threadPoolName = threadPoolName;
    this.delegate = createThreadPoolExecutor(threadPoolSettings);

    statsDSender.sendMetricPeriodically(getFullMetricName(serviceName, "size"), () -> (long) delegate.getPoolSize());
    statsDSender.sendMetricPeriodically(getFullMetricName(serviceName, "activeCount"), () -> (long) delegate.getActiveCount());
    statsDSender.sendMetricPeriodically(getFullMetricName(serviceName, "queueSize"), () -> (long) delegate.getQueue().size());
  }

  @Override
  public void execute(Runnable command) {
    delegate.execute(command);
  }

  private ThreadPoolExecutor createThreadPoolExecutor(FileSettings threadPoolSettings) {
    var defaultThreadFactory = Executors.defaultThreadFactory();
    var count = new AtomicLong(0);

    var threadFactory = new ThreadFactory() {
      @Override
      public Thread newThread(Runnable r) {
        Thread thread = defaultThreadFactory.newThread(r);
        thread.setName(threadPoolName + String.format("-monitored-pool-thread-%s", count.getAndIncrement()));
        thread.setDaemon(true);
        return thread;
      }
    };

    int minThreads = ofNullable(threadPoolSettings.getInteger("minSize")).orElse(8);
    int maxThreads = ofNullable(threadPoolSettings.getInteger("maxSize")).orElse(16);
    int queueSize = ofNullable(threadPoolSettings.getInteger("queueSize")).orElse(16);
    int keepAliveTimeSec = ofNullable(threadPoolSettings.getInteger("keepAliveTimeSec")).orElse(60);

    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
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
