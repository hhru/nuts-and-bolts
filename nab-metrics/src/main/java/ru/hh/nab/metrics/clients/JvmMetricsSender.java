package ru.hh.nab.metrics.clients;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.Tag;

public class JvmMetricsSender {
  private static final String COMPRESSED_CLASS_SPACE_POOL = "Compressed Class Space";
  private static final String METASPACE_POOL = "Metaspace";
  private static final String POOL_TAG_NAME = "pool";
  private static final String OTHER_METADATA_POOL = "Other metadata";  // Metaspace = Compressed Class Space + Other metadata
  private static final Tag OTHER_METADATA_TAG = new Tag(POOL_TAG_NAME, OTHER_METADATA_POOL);
  private static final Tag TOTAL_TAG = new Tag(POOL_TAG_NAME, "total");
  private static final MemoryUsage ZERO_MEMORY_USAGE = new MemoryUsage(0, 0, 0, 0);

  public static final String HEAP_USED_METRIC_NAME = "jvm.heap.used";
  public static final String HEAP_MAX_METRIC_NAME = "jvm.heap.max";
  public static final String HEAP_COMMITED_METRIC_NAME = "jvm.heap.commited";

  public static final String NON_HEAP_USED_METRIC_NAME = "jvm.nonHeap.used";
  public static final String NON_HEAP_MAX_METRIC_NAME = "jvm.nonHeap.max";
  public static final String NON_HEAP_COMMITED_METRIC_NAME = "jvm.nonHeap.commited";

  public static final String BUFFER_POOL_USED_METRIC_NAME = "jvm.bufferPool.used";
  public static final String BUFFER_POOL_CAPACITY_METRIC_NAME = "jvm.bufferPool.capacity";

  public static final String THREAD_COUNT_METRIC_NAME = "jvm.threads";

  public static final String LOADED_CLASSES_COUNT_METRIC_NAME = "jvm.loadedClasses";
  public static final String TOTAL_LOADED_CLASSES_COUNT_METRIC_NAME = "jvm.classes.loaded.total.count";
  public static final String TOTAL_UNLOADED_CLASSES_COUNT_METRIC_NAME = "jvm.classes.unloaded.total.count";

  public static final String TOTAL_COMPILATION_TIME_METRIC_NAME = "jvm.compilation.time.total.ms";

  private final StatsDSender statsDSender;
  private final Tag appTag;
  private final MemoryMXBean memoryMXBean;
  private final ClassLoadingMXBean classLoadingMXBean;
  private final ThreadMXBean threadMXBean;
  private final CompilationMXBean compilationMXBean;
  private final boolean isCompilationTimeMonitoringSupported;

  public JvmMetricsSender(StatsDSender statsDSender, String serviceName) {
    this.statsDSender = statsDSender;
    this.appTag = new Tag(Tag.APP_TAG_NAME, serviceName);
    this.memoryMXBean = ManagementFactory.getMemoryMXBean();
    this.classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
    this.threadMXBean = ManagementFactory.getThreadMXBean();
    this.compilationMXBean = ManagementFactory.getCompilationMXBean();
    this.isCompilationTimeMonitoringSupported = compilationMXBean != null && compilationMXBean.isCompilationTimeMonitoringSupported();

    statsDSender.sendPeriodically(this::sendJvmMetrics);
  }

  void sendJvmMetrics() {
    MemoryUsage compressedClassSpaceUsage = ZERO_MEMORY_USAGE, metaspaceUsage = null;

    for (MemoryPoolMXBean memoryPool : ManagementFactory.getMemoryPoolMXBeans()) {
      String poolName = memoryPool.getName();
      MemoryUsage memoryUsage = memoryPool.getUsage();

      if (memoryPool.getType() == MemoryType.HEAP) {
        sendHeapMemoryPoolUsage(memoryUsage, new Tag(POOL_TAG_NAME, poolName));
      } else {
        if (poolName.equals(COMPRESSED_CLASS_SPACE_POOL)) {
          compressedClassSpaceUsage = memoryUsage;
        }

        if (poolName.equals(METASPACE_POOL)) {
          metaspaceUsage = memoryUsage;
        } else {
          sendNonHeapMemoryPoolUsage(memoryUsage, new Tag(POOL_TAG_NAME, poolName));
        }
      }
    }

    sendHeapMemoryPoolUsage(memoryMXBean.getHeapMemoryUsage(), TOTAL_TAG);

    // There are separate JMX metrics for pools "Metaspace" and "Compressed Class Space", but actually CCS is a part of Metaspace.
    // So we subtract CCS from Metaspace to get a more meaningful pool metric "Other metadata".
    // Also, JMX apparently counts CCS twice in the total non-heap usage (getNonHeapMemoryUsage), so we correct that too.
    sendNonHeapPoolMemoryUsageAdjusted(memoryMXBean.getNonHeapMemoryUsage(), compressedClassSpaceUsage, TOTAL_TAG);
    if (metaspaceUsage != null) {
      sendNonHeapPoolMemoryUsageAdjusted(metaspaceUsage, compressedClassSpaceUsage, OTHER_METADATA_TAG);
    }

    sendBufferPoolsUsage();

    sendClassLoadingMetrics();
    sendCompilationMetrics();
    statsDSender.sendGauge(THREAD_COUNT_METRIC_NAME, threadMXBean.getThreadCount(), appTag);
  }

  private void sendClassLoadingMetrics() {
    statsDSender.sendGauge(LOADED_CLASSES_COUNT_METRIC_NAME, classLoadingMXBean.getLoadedClassCount(), appTag);
    statsDSender.sendGauge(TOTAL_LOADED_CLASSES_COUNT_METRIC_NAME, classLoadingMXBean.getTotalLoadedClassCount(), appTag);
    statsDSender.sendGauge(TOTAL_UNLOADED_CLASSES_COUNT_METRIC_NAME, classLoadingMXBean.getUnloadedClassCount(), appTag);
  }

  private void sendCompilationMetrics() {
    if (!isCompilationTimeMonitoringSupported) {
      return;
    }

    statsDSender.sendGauge(TOTAL_COMPILATION_TIME_METRIC_NAME, compilationMXBean.getTotalCompilationTime(), appTag);
  }

  private void sendHeapMemoryPoolUsage(MemoryUsage poolUsage, Tag poolTag) {
    statsDSender.sendGauge(HEAP_USED_METRIC_NAME, poolUsage.getUsed(), appTag, poolTag);
    statsDSender.sendGauge(HEAP_MAX_METRIC_NAME, poolUsage.getMax(), appTag, poolTag);
    statsDSender.sendGauge(HEAP_COMMITED_METRIC_NAME, poolUsage.getCommitted(), appTag, poolTag);
  }

  private void sendNonHeapMemoryPoolUsage(MemoryUsage poolUsage, Tag poolTag) {
    statsDSender.sendGauge(NON_HEAP_USED_METRIC_NAME, poolUsage.getUsed(), appTag, poolTag);
    statsDSender.sendGauge(NON_HEAP_MAX_METRIC_NAME, poolUsage.getMax(), appTag, poolTag);
    statsDSender.sendGauge(NON_HEAP_COMMITED_METRIC_NAME, poolUsage.getCommitted(), appTag, poolTag);
  }

  private void sendNonHeapPoolMemoryUsageAdjusted(MemoryUsage poolUsage, MemoryUsage excludedUsage, Tag tag) {
    statsDSender.sendGauge(NON_HEAP_USED_METRIC_NAME, poolUsage.getUsed() - excludedUsage.getUsed(), appTag, tag);
    statsDSender.sendGauge(NON_HEAP_COMMITED_METRIC_NAME, poolUsage.getCommitted() - excludedUsage.getCommitted(), appTag, tag);
    statsDSender.sendGauge(NON_HEAP_MAX_METRIC_NAME, Math.max(-1, poolUsage.getMax() - excludedUsage.getMax()), appTag, tag);
  }

  private void sendBufferPoolsUsage() {
    for (BufferPoolMXBean bufferPool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
      Tag poolTag = new Tag(POOL_TAG_NAME, bufferPool.getName());
      statsDSender.sendGauge(BUFFER_POOL_USED_METRIC_NAME, bufferPool.getMemoryUsed(), appTag, poolTag);
      statsDSender.sendGauge(BUFFER_POOL_CAPACITY_METRIC_NAME, bufferPool.getTotalCapacity(), appTag, poolTag);
    }
  }
}
