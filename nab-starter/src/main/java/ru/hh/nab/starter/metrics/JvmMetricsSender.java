package ru.hh.nab.starter.metrics;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.Tag;

public class JvmMetricsSender {
  private static final String COMPRESSED_CLASS_SPACE_POOL = "Compressed Class Space";
  private static final String METASPACE_POOL = "Metaspace";
  private static final String POOL_TAG_NAME = "pool";
  private static final String METADATA_POOL = "Metadata";  // Metaspace = Metadata + Compressed Class Space
  private static final Tag METASPACE_TAG = new Tag(POOL_TAG_NAME, METADATA_POOL);
  private static final Tag TOTAL_TAG = new Tag(POOL_TAG_NAME, "total");
  private static final MemoryUsage ZERO_MEMORY_USAGE = new MemoryUsage(0, 0, 0, 0);

  private final StatsDSender statsDSender;
  private final Tag appTag;
  private final MemoryMXBean memoryMXBean;
  private final String heapUsedMetricName;
  private final String heapMaxMetricName;
  private final String heapCommitedMetricName;
  private final String nonHeapUsedMetricName;
  private final String nonHeapMaxMetricName;
  private final String nonHeapCommitedMetricName;
  private final String bufferPoolUsedMetricName;
  private final String bufferPoolCapacityMetricName;
  private final String threadCountMetricName;
  private final String loadedClassesCountMetricName;

  private JvmMetricsSender(StatsDSender statsDSender, String serviceName) {
    this.statsDSender = statsDSender;
    this.appTag = new Tag(Tag.APP_TAG_NAME, serviceName);
    this.memoryMXBean = ManagementFactory.getMemoryMXBean();
    this.heapUsedMetricName = "jvm.heap.used";
    this.heapMaxMetricName = "jvm.heap.max";
    this.heapCommitedMetricName = "jvm.heap.commited";
    this.nonHeapUsedMetricName = "jvm.nonHeap.used";
    this.nonHeapMaxMetricName = "jvm.nonHeap.max";
    this.nonHeapCommitedMetricName = "jvm.nonHeap.commited";
    this.bufferPoolUsedMetricName = "jvm.bufferPool.used";
    this.bufferPoolCapacityMetricName = "jvm.bufferPool.capacity";
    this.threadCountMetricName = "jvm.threads";
    this.loadedClassesCountMetricName = "jvm.loadedClasses";
  }

  public static void create(StatsDSender statsDSender, String serviceName) {
    JvmMetricsSender jvmMetricsSender = new JvmMetricsSender(statsDSender, serviceName);
    statsDSender.sendPeriodically(jvmMetricsSender::sendJvmMetrics);
  }

  private void sendJvmMetrics() {
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

    sendNonHeapPoolMemoryUsageAdjusted(memoryMXBean.getNonHeapMemoryUsage(), compressedClassSpaceUsage, TOTAL_TAG);
    if (metaspaceUsage != null) {
      
      sendNonHeapPoolMemoryUsageAdjusted(metaspaceUsage, compressedClassSpaceUsage, METASPACE_TAG);
    }

    sendBufferPoolsUsage();

    statsDSender.sendGauge(loadedClassesCountMetricName, ManagementFactory.getClassLoadingMXBean().getLoadedClassCount(), appTag);
    statsDSender.sendGauge(threadCountMetricName, ManagementFactory.getThreadMXBean().getThreadCount(), appTag);
  }

  private void sendHeapMemoryPoolUsage(MemoryUsage poolUsage, Tag poolTag) {
    statsDSender.sendGauge(heapUsedMetricName, poolUsage.getUsed(), appTag, poolTag);
    statsDSender.sendGauge(heapMaxMetricName, poolUsage.getMax(), appTag, poolTag);
    statsDSender.sendGauge(heapCommitedMetricName, poolUsage.getCommitted(), appTag, poolTag);
  }

  private void sendNonHeapMemoryPoolUsage(MemoryUsage poolUsage, Tag poolTag) {
    statsDSender.sendGauge(nonHeapUsedMetricName, poolUsage.getUsed(), appTag, poolTag);
    statsDSender.sendGauge(nonHeapMaxMetricName, poolUsage.getMax(), appTag, poolTag);
    statsDSender.sendGauge(nonHeapCommitedMetricName, poolUsage.getCommitted(), appTag, poolTag);
  }

  // Non-heap usage JMX metric sums up Metaspace and Compressed Class Space, but in fact CCS is included in Metaspace
  private void sendNonHeapPoolMemoryUsageAdjusted(MemoryUsage poolUsage, MemoryUsage ccsUsage, Tag tag) {
    statsDSender.sendGauge(nonHeapUsedMetricName, poolUsage.getUsed() - ccsUsage.getUsed(), appTag, tag);
    statsDSender.sendGauge(nonHeapCommitedMetricName, poolUsage.getCommitted() - ccsUsage.getCommitted(), appTag, tag);
    statsDSender.sendGauge(nonHeapMaxMetricName, Math.max(-1, poolUsage.getMax() - ccsUsage.getMax()), appTag, tag);
  }

  private void sendBufferPoolsUsage() {
    for (BufferPoolMXBean bufferPool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
      Tag poolTag = new Tag(POOL_TAG_NAME, bufferPool.getName());
      statsDSender.sendGauge(bufferPoolUsedMetricName, bufferPool.getMemoryUsed(), appTag, poolTag);
      statsDSender.sendGauge(bufferPoolCapacityMetricName, bufferPool.getTotalCapacity(), appTag, poolTag);
    }
  }
}
