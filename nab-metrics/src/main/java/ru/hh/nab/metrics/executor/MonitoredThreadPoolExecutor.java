package ru.hh.nab.metrics.executor;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Properties;
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
import ru.hh.nab.common.properties.PropertiesUtils;
import ru.hh.nab.metrics.CompactHistogram;
import ru.hh.nab.metrics.Histogram;
import ru.hh.nab.metrics.Max;
import ru.hh.nab.metrics.StatsDSender;
import static ru.hh.nab.metrics.StatsDSender.DEFAULT_PERCENTILES;
import ru.hh.nab.metrics.Tag;
import ru.hh.nab.metrics.TaggedSender;

public class MonitoredThreadPoolExecutor extends ThreadPoolExecutor {

  public static final String MIN_SIZE_PROPERTY = "minSize";
  public static final String MAX_SIZE_PROPERTY = "maxSize";
  public static final String QUEUE_SIZE_PROPERTY = "queueSize";
  public static final String KEEP_ALIVE_TIME_SEC_PROPERTY = "keepAliveTimeSec";
  public static final String LONG_TASK_DURATION_MS_PROPERTY = "longTaskDurationMs";
  public static final String MONITORING_TASK_DURATION_HISTOGRAM_SIZE_PROPERTY = "monitoring.taskDuration.histogramSize";
  public static final String MONITORING_TASK_DURATION_HISTOGRAM_COMPACTION_RATIO_PROPERTY = "monitoring.taskDuration.histogramCompactionRatio";
  public static final String MONITORING_TASK_EXECUTION_START_LAG_HISTOGRAM_SIZE_PROPERTY = "monitoring.taskExecutionStartLag.histogramSize";
  public static final String MONITORING_TASK_EXECUTION_START_LAG_HISTOGRAM_COMPACTION_RATIO_PROPERTY =
      "monitoring.taskExecutionStartLag.histogramCompactionRatio";

  private static final Logger LOGGER = LoggerFactory.getLogger(MonitoredThreadPoolExecutor.class);
  private static final ThreadFactory DEFAULT_THREAD_FACTORY = defaultThreadFactory();

  private final Max poolSizeMetric = new Max(0);
  private final Max activeCountMetric = new Max(0);
  private final Max queueSizeMetric = new Max(0);
  private final Histogram taskDurationMetric;
  private final Histogram taskExecutionStartLagMetric;
  private final ThreadLocal<Long> taskStart = new ThreadLocal<>();
  private final String threadPoolName;
  private final Integer longTaskDurationMs;

  private MonitoredThreadPoolExecutor(
      int corePoolSize,
      int maximumPoolSize,
      long keepAliveTime,
      TimeUnit unit,
      BlockingQueue<Runnable> workQueue,
      ThreadFactory threadFactory,
      RejectedExecutionHandler handler,
      String threadPoolName,
      Integer longTaskDurationMs,
      int taskDurationHistogramSize,
      int taskDurationHistogramCompactionRatio,
      int taskExecutionStartLagHistogramSize,
      int taskExecutionStartLagHistogramCompactionRatio
  ) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);

    this.longTaskDurationMs = longTaskDurationMs;
    this.threadPoolName = threadPoolName;
    this.taskDurationMetric = new CompactHistogram(taskDurationHistogramSize, taskDurationHistogramCompactionRatio);
    this.taskExecutionStartLagMetric = new CompactHistogram(taskExecutionStartLagHistogramSize, taskExecutionStartLagHistogramCompactionRatio);
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

  public static ThreadPoolExecutor create(Properties threadPoolProperties, String threadPoolName, StatsDSender statsDSender, String serviceName) {
    return create(threadPoolProperties, threadPoolName, statsDSender, serviceName, (r, executor) -> {
      LOGGER.warn(
          "{} thread pool is low on threads: size={}, activeCount={}, queueSize={}",
          threadPoolName,
          executor.getPoolSize(),
          executor.getActiveCount(),
          executor.getQueue().size()
      );
      throw new RejectedExecutionException(threadPoolName + " thread pool is low on threads");
    });
  }

  public static ThreadPoolExecutor create(
      Properties threadPoolProperties,
      String threadPoolName,
      StatsDSender statsDSender,
      String serviceName,
      RejectedExecutionHandler rejectedExecutionHandler
  ) {
    int coreThreads = PropertiesUtils.getInteger(threadPoolProperties, MIN_SIZE_PROPERTY, 4);
    int maxThreads = PropertiesUtils.getInteger(threadPoolProperties, MAX_SIZE_PROPERTY, 16);
    int queueSize = PropertiesUtils.getInteger(threadPoolProperties, QUEUE_SIZE_PROPERTY, maxThreads);
    int keepAliveTimeSec = PropertiesUtils.getInteger(threadPoolProperties, KEEP_ALIVE_TIME_SEC_PROPERTY, 60);
    Integer longTaskDurationMs = PropertiesUtils.getInteger(threadPoolProperties, LONG_TASK_DURATION_MS_PROPERTY);
    int taskDurationHistogramSize = PropertiesUtils.getInteger(threadPoolProperties, MONITORING_TASK_DURATION_HISTOGRAM_SIZE_PROPERTY, 512);
    int taskDurationHistogramCompactionRatio =
        PropertiesUtils.getInteger(threadPoolProperties, MONITORING_TASK_DURATION_HISTOGRAM_COMPACTION_RATIO_PROPERTY, 1);
    int taskExecutionStartLagHistogramSize =
        PropertiesUtils.getInteger(threadPoolProperties, MONITORING_TASK_EXECUTION_START_LAG_HISTOGRAM_SIZE_PROPERTY, 512);
    int taskExecutionStartLagHistogramCompactionRatio =
        PropertiesUtils.getInteger(threadPoolProperties, MONITORING_TASK_EXECUTION_START_LAG_HISTOGRAM_COMPACTION_RATIO_PROPERTY, 1);

    var count = new AtomicLong(0);
    ThreadFactory threadFactory = r -> {
      Thread thread = DEFAULT_THREAD_FACTORY.newThread(r);
      thread.setName(String.format("%s-%s", threadPoolName, count.getAndIncrement()));
      thread.setDaemon(true);
      return thread;
    };

    var threadPoolExecutor = new MonitoredThreadPoolExecutor(
        coreThreads,
        maxThreads,
        keepAliveTimeSec,
        TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(queueSize),
        threadFactory,
        rejectedExecutionHandler,
        threadPoolName,
        longTaskDurationMs,
        taskDurationHistogramSize,
        taskDurationHistogramCompactionRatio,
        taskExecutionStartLagHistogramSize,
        taskExecutionStartLagHistogramCompactionRatio
    );

    String maxPoolSizeMetricName = "threadPool.maxSize";
    String poolSizeMetricName = "threadPool.size";
    String activeCountMetricName = "threadPool.activeCount";
    String queueSizeMetricName = "threadPool.queueSize";
    String taskDurationMetricName = "threadPool.taskDuration";
    String taskExecutionStartLagMetricName = "threadPool.taskExecutionStartLag";
    var sender = new TaggedSender(statsDSender, Set.of(new Tag(Tag.APP_TAG_NAME, serviceName), new Tag("pool", threadPoolName)));

    statsDSender.sendPeriodically(() -> {
      sender.sendGauge(maxPoolSizeMetricName, threadPoolExecutor.getMaximumPoolSize());
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
