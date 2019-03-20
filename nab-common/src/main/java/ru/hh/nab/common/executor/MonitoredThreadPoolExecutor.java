package ru.hh.nab.common.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.metrics.Histogram;
import ru.hh.metrics.Max;
import ru.hh.metrics.StatsDSender;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.Executors.defaultThreadFactory;

public class MonitoredThreadPoolExecutor extends ThreadPoolExecutor {
  private static final Logger LOGGER = LoggerFactory.getLogger(MonitoredThreadPoolExecutor.class);
  private static final ThreadFactory DEFAULT_THREAD_FACTORY = defaultThreadFactory();

  private final Max poolSizeMetric = new Max(0);
  private final Max activeCountMetric = new Max(0);
  private final Max queueSizeMetric = new Max(0);
  private final Histogram taskDurationMetric = new Histogram(500);
  private ThreadLocal<Long> taskStart = new ThreadLocal<>();
  private final String threadPoolName;
  private final Integer longTaskDurationMs;

  private MonitoredThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue,
                                      ThreadFactory threadFactory, RejectedExecutionHandler handler,
                                      String threadPoolName, Integer longTaskDurationMs) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);

    this.longTaskDurationMs = longTaskDurationMs;
    this.threadPoolName = threadPoolName;
  }

  @Override
  protected void beforeExecute(Thread t, Runnable r) {
    poolSizeMetric.save(getPoolSize());
    activeCountMetric.save(getActiveCount());
    queueSizeMetric.save(getQueue().size());

    taskStart.set(System.currentTimeMillis());
  }

  @Override
  protected void afterExecute(Runnable r, Throwable t) {
    int taskDuration = (int) (System.currentTimeMillis() - taskStart.get());

    taskDurationMetric.save(taskDuration);

    if (longTaskDurationMs != null && longTaskDurationMs >= taskDuration) {
      LOGGER.warn("{} thread pool task execution took too long: {} >= {} ms", threadPoolName, taskDuration, longTaskDurationMs);
    }
  }

  public static ThreadPoolExecutor create(FileSettings threadPoolSettings, String threadPoolName, StatsDSender statsDSender, String serviceName) {
    int coreThreads = ofNullable(threadPoolSettings.getInteger("minSize")).orElse(8);
    int maxThreads = ofNullable(threadPoolSettings.getInteger("maxSize")).orElse(16);
    int queueSize = ofNullable(threadPoolSettings.getInteger("queueSize")).orElse(16);
    int keepAliveTimeSec = ofNullable(threadPoolSettings.getInteger("keepAliveTimeSec")).orElse(60);
    Integer longTaskDurationMs = ofNullable(threadPoolSettings.getInteger("longTaskDurationMs")).orElse(null);

    var count = new AtomicLong(0);
    ThreadFactory threadFactory = r -> {
      Thread thread = DEFAULT_THREAD_FACTORY.newThread(r);
      thread.setName(String.format("%s-%s", threadPoolName, count.getAndIncrement()));
      thread.setDaemon(true);
      return thread;
    };

    var threadPoolExecutor = new MonitoredThreadPoolExecutor(
      coreThreads, maxThreads, keepAliveTimeSec, TimeUnit.SECONDS, new ArrayBlockingQueue<>(queueSize), threadFactory,
      (r, executor) -> {
        LOGGER.warn("{} thread pool is low on threads: size={}, activeCount={}, queueSize={}",
          threadPoolName, executor.getPoolSize(), executor.getActiveCount(), executor.getQueue().size());
        throw new RejectedExecutionException(threadPoolName + " thread pool is low on threads");
      },
      threadPoolName, longTaskDurationMs
    );

    statsDSender.sendMaxPeriodically(getFullMetricName(serviceName, threadPoolName, "size"), threadPoolExecutor.poolSizeMetric);
    statsDSender.sendMaxPeriodically(getFullMetricName(serviceName, threadPoolName, "activeCount"), threadPoolExecutor.activeCountMetric);
    statsDSender.sendMaxPeriodically(getFullMetricName(serviceName, threadPoolName, "queueSize"), threadPoolExecutor.queueSizeMetric);
    statsDSender.sendPercentilesPeriodically(
      getFullMetricName(serviceName, threadPoolName, "taskDuration"), threadPoolExecutor.taskDurationMetric, 95, 99, 100
    );

    threadPoolExecutor.prestartAllCoreThreads();
    return threadPoolExecutor;
  }

  private static String getFullMetricName(String serviceName, String threadPoolName, String shortMetricName) {
    return serviceName + '.' + threadPoolName + ".threadPool." + shortMetricName;
  }
}
