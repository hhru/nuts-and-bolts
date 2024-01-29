package ru.hh.nab.common.executor;

import java.time.Duration;
import java.time.OffsetDateTime;
import static java.util.Optional.ofNullable;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import static java.util.concurrent.Executors.defaultThreadFactory;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.metrics.CompactHistogram;
import ru.hh.nab.metrics.Histogram;
import ru.hh.nab.metrics.Max;
import ru.hh.nab.metrics.SimpleHistogram;
import ru.hh.nab.metrics.StatsDSender;
import static ru.hh.nab.metrics.StatsDSender.DEFAULT_PERCENTILES;
import ru.hh.nab.metrics.Tag;
import ru.hh.nab.metrics.TaggedSender;

public class MonitoredThreadPoolExecutor extends ThreadPoolExecutor {
  private static final Logger LOGGER = LoggerFactory.getLogger(MonitoredThreadPoolExecutor.class);
  private static final ThreadFactory DEFAULT_THREAD_FACTORY = defaultThreadFactory();

  private final Max maxPoolSizeMetric = new Max(0);
  private final Max poolSizeMetric = new Max(0);
  private final Max activeCountMetric = new Max(0);
  private final Max queueSizeMetric = new Max(0);
  private final Histogram taskDurationMetric = new CompactHistogram(512, 32);
  private final Histogram taskExecutionStartLagMetric = new SimpleHistogram(500);
  private final ThreadLocal<Long> taskStart = new ThreadLocal<>();
  private final String threadPoolName;
  private final Integer longTaskDurationMs;

  private MonitoredThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue,
                                      ThreadFactory threadFactory, RejectedExecutionHandler handler,
                                      String threadPoolName, Integer longTaskDurationMs) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);

    this.longTaskDurationMs = longTaskDurationMs;
    this.threadPoolName = threadPoolName;
  }

  public String getThreadPoolName() {
    return threadPoolName;
  }

  @Override
  public void execute(Runnable command) {
    super.execute(new RunnableWithCreationTime(command));
  }

  @Override
  protected void beforeExecute(Thread t, Runnable r) {
    maxPoolSizeMetric.save(getMaximumPoolSize());
    poolSizeMetric.save(getPoolSize());
    activeCountMetric.save(getActiveCount());
    queueSizeMetric.save(getQueue().size());

    if (r instanceof RunnableWithCreationTime rWithCreationTime) {
      int taskExecutionStartLag = (int) Duration.between(rWithCreationTime.getCreationTime(), OffsetDateTime.now()).toMillis();
      taskExecutionStartLagMetric.save(taskExecutionStartLag);
    }
    taskStart.set(System.currentTimeMillis());
  }

  @Override
  protected void afterExecute(Runnable r, Throwable t) {
    int taskDuration = (int) (System.currentTimeMillis() - taskStart.get());

    taskDurationMetric.save(taskDuration);

    if (longTaskDurationMs != null && taskDuration >= longTaskDurationMs) {
      LOGGER.warn("{} thread pool task execution took too long: {} ms >= {} ms", threadPoolName, taskDuration, longTaskDurationMs);
    }
  }

  public static ThreadPoolExecutor create(FileSettings threadPoolSettings, String threadPoolName, StatsDSender statsDSender, String serviceName) {
    return create(threadPoolSettings, threadPoolName, statsDSender, serviceName, (r, executor) -> {
      LOGGER.warn("{} thread pool is low on threads: size={}, activeCount={}, queueSize={}",
          threadPoolName, executor.getPoolSize(), executor.getActiveCount(), executor.getQueue().size());
      throw new RejectedExecutionException(threadPoolName + " thread pool is low on threads");
    });
  }

  public static ThreadPoolExecutor create(FileSettings threadPoolSettings, String threadPoolName, StatsDSender statsDSender, String serviceName,
                                          RejectedExecutionHandler rejectedExecutionHandler) {
    int coreThreads = ofNullable(threadPoolSettings.getInteger("minSize")).orElse(4);
    int maxThreads = ofNullable(threadPoolSettings.getInteger("maxSize")).orElse(16);
    int queueSize = ofNullable(threadPoolSettings.getInteger("queueSize")).orElse(maxThreads);
    int keepAliveTimeSec = ofNullable(threadPoolSettings.getInteger("keepAliveTimeSec")).orElse(60);
    Integer longTaskDurationMs = ofNullable(threadPoolSettings.getInteger("longTaskDurationMs")).orElse(null);

    var count = new AtomicLong(0);
    ThreadFactory threadFactory = r -> {
      Thread thread = DEFAULT_THREAD_FACTORY.newThread(r);
      thread.setName(String.format("%s-%s", threadPoolName, count.getAndIncrement()));
      thread.setDaemon(true);
      return thread;
    };

    var threadPoolExecutor = new MonitoredThreadPoolExecutor(coreThreads, maxThreads, keepAliveTimeSec, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(queueSize), threadFactory, rejectedExecutionHandler, threadPoolName, longTaskDurationMs
    );

    String maxPoolSizeMetricName = "threadPool.maxSize";
    String poolSizeMetricName = "threadPool.size";
    String activeCountMetricName = "threadPool.activeCount";
    String queueSizeMetricName = "threadPool.queueSize";
    String taskDurationMetricName = "threadPool.taskDuration";
    String taskExecutionStartLagMetricName = "threadPool.taskExecutionStartLag";
    var sender = new TaggedSender(statsDSender, Set.of(new Tag(Tag.APP_TAG_NAME, serviceName), new Tag("pool", threadPoolName)));

    statsDSender.sendPeriodically(() -> {
      sender.sendMax(maxPoolSizeMetricName, threadPoolExecutor.maxPoolSizeMetric);
      sender.sendMax(poolSizeMetricName, threadPoolExecutor.poolSizeMetric);
      sender.sendMax(activeCountMetricName, threadPoolExecutor.activeCountMetric);
      sender.sendMax(queueSizeMetricName, threadPoolExecutor.queueSizeMetric);
      sender.sendHistogram(taskDurationMetricName, threadPoolExecutor.taskDurationMetric, DEFAULT_PERCENTILES);
      sender.sendHistogram(taskExecutionStartLagMetricName, threadPoolExecutor.taskExecutionStartLagMetric, DEFAULT_PERCENTILES);
    });

    threadPoolExecutor.prestartAllCoreThreads();
    return threadPoolExecutor;
  }

  private static class RunnableWithCreationTime implements Runnable {
    private final Runnable runnable;
    private final OffsetDateTime creationTime = OffsetDateTime.now();

    public RunnableWithCreationTime(Runnable runnable) {
      this.runnable = runnable;
    }

    @Override
    public void run() {
      runnable.run();
    }


    public Runnable getRunnable() {
      return runnable;
    }

    public OffsetDateTime getCreationTime() {
      return creationTime;
    }
  }
}
