package ru.hh.nab.common.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.metrics.Max;
import ru.hh.metrics.StatsDSender;
import ru.hh.nab.common.properties.FileSettings;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.Executors.defaultThreadFactory;

public class MonitoredThreadPoolExecutor implements Executor {
  private final static Logger LOGGER = LoggerFactory.getLogger(MonitoredThreadPoolExecutor.class);

  private final ThreadPoolExecutor delegate;
  private final String threadPoolName;
  private final Max poolSizeMax;
  private final Max activeCountMax;
  private final Max queueSizeMax;

  public MonitoredThreadPoolExecutor(String threadPoolName, String serviceName, FileSettings threadPoolSettings, StatsDSender statsDSender) {
    this.poolSizeMax = new Max(0);
    this.activeCountMax = new Max(0);
    this.queueSizeMax = new Max(0);

    this.threadPoolName = threadPoolName;
    this.delegate = createThreadPoolExecutor(threadPoolSettings);

    statsDSender.sendMaxPeriodically(getFullMetricName(serviceName, "size"), poolSizeMax);
    statsDSender.sendMaxPeriodically(getFullMetricName(serviceName, "activeCount"), activeCountMax);
    statsDSender.sendMaxPeriodically(getFullMetricName(serviceName, "queueSize"), queueSizeMax);
  }

  @Override
  public void execute(Runnable command) {
    delegate.execute(command);
  }

  private ThreadPoolExecutor createThreadPoolExecutor(FileSettings threadPoolSettings) {
    var defaultThreadFactory = defaultThreadFactory();
    var count = new AtomicLong(0);

    int minThreads = ofNullable(threadPoolSettings.getInteger("minSize")).orElse(8);
    int maxThreads = ofNullable(threadPoolSettings.getInteger("maxSize")).orElse(16);
    int queueSize = ofNullable(threadPoolSettings.getInteger("queueSize")).orElse(16);
    int keepAliveTimeSec = ofNullable(threadPoolSettings.getInteger("keepAliveTimeSec")).orElse(60);

    var threadPoolExecutor = new ThreadPoolExecutor(
      minThreads, maxThreads, keepAliveTimeSec, TimeUnit.SECONDS, new ArrayBlockingQueue<>(queueSize), defaultThreadFactory(),
      (r, executor) -> {
        LOGGER.warn("{} thread pool is low on threads: size={}, activeCount={}, queueSize={}",
          threadPoolName, executor.getPoolSize(), executor.getActiveCount(), executor.getQueue().size());
        throw new RejectedExecutionException(threadPoolName + " thread pool is low on threads");
      });

    threadPoolExecutor.setThreadFactory(r -> {
      poolSizeMax.save(threadPoolExecutor.getPoolSize());
      activeCountMax.save(threadPoolExecutor.getActiveCount());
      queueSizeMax.save(threadPoolExecutor.getQueue().size());

      Thread thread = defaultThreadFactory.newThread(r);
      thread.setName(String.format("%s-monitored-pool-thread-%s", threadPoolName, count.getAndIncrement()));
      thread.setDaemon(true);
      return thread;
    });

    threadPoolExecutor.prestartAllCoreThreads();
    return threadPoolExecutor;
  }

  private String getFullMetricName(String serviceName, String shortMetricName) {
    return serviceName + '.' + threadPoolName + ".threadPool." + shortMetricName;
  }
}
