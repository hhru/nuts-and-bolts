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
  private static final String METADATA_POOL = "Metadata";  // Metaspace = Metadata + Compressed Class Space

  private static final Tag TOTAL_TAG = new Tag("pool", "total");
  private static final MemoryUsage ZERO_MEMORY_USAGE = new MemoryUsage(0, 0, 0, 0);

  private final StatsDSender statsDSender;
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
    this.memoryMXBean = ManagementFactory.getMemoryMXBean();
    this.heapUsedMetricName = getFullMetricName(serviceName, "heap.used");
    this.heapMaxMetricName = getFullMetricName(serviceName, "heap.max");
    this.heapCommitedMetricName = getFullMetricName(serviceName, "heap.commited");
    this.nonHeapUsedMetricName = getFullMetricName(serviceName, "nonHeap.used");
    this.nonHeapMaxMetricName = getFullMetricName(serviceName, "nonHeap.max");
    this.nonHeapCommitedMetricName = getFullMetricName(serviceName, "nonHeap.commited");
    this.bufferPoolUsedMetricName = getFullMetricName(serviceName, "bufferPool.used");
    this.bufferPoolCapacityMetricName = getFullMetricName(serviceName, "bufferPool.capacity");
    this.threadCountMetricName = getFullMetricName(serviceName, "threads");
    this.loadedClassesCountMetricName = getFullMetricName(serviceName, "loadedClasses");
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
        sendHeapMemoryPoolUsage(memoryUsage, new Tag("pool", poolName));
      } else {
        if (poolName.equals(COMPRESSED_CLASS_SPACE_POOL)) {
          compressedClassSpaceUsage = memoryUsage;
        }

        if (poolName.equals(METASPACE_POOL)) {
          metaspaceUsage = memoryUsage;
        } else {
          sendNonHeapMemoryPoolUsage(memoryUsage, new Tag("pool", poolName));
        }
      }
    }

    sendHeapMemoryPoolUsage(memoryMXBean.getHeapMemoryUsage(), TOTAL_TAG);

    sendNonHeapPoolMemoryUsageAdjusted(memoryMXBean.getNonHeapMemoryUsage(), compressedClassSpaceUsage, TOTAL_TAG);
    if (metaspaceUsage != null) {
      sendNonHeapPoolMemoryUsageAdjusted(metaspaceUsage, compressedClassSpaceUsage, new Tag("pool", METADATA_POOL));
    }

    sendBufferPoolsUsage();

    statsDSender.sendGauge(loadedClassesCountMetricName, ManagementFactory.getClassLoadingMXBean().getLoadedClassCount());
    statsDSender.sendGauge(threadCountMetricName, ManagementFactory.getThreadMXBean().getThreadCount());
  }

  private void sendHeapMemoryPoolUsage(MemoryUsage poolUsage, Tag poolTag) {
    statsDSender.sendGauge(heapUsedMetricName, poolUsage.getUsed(), poolTag);
    statsDSender.sendGauge(heapMaxMetricName, poolUsage.getMax(), poolTag);
    statsDSender.sendGauge(heapCommitedMetricName, poolUsage.getCommitted(), poolTag);
  }

  private void sendNonHeapMemoryPoolUsage(MemoryUsage poolUsage, Tag poolTag) {
    statsDSender.sendGauge(nonHeapUsedMetricName, poolUsage.getUsed(), poolTag);
    statsDSender.sendGauge(nonHeapMaxMetricName, poolUsage.getMax(), poolTag);
    statsDSender.sendGauge(nonHeapCommitedMetricName, poolUsage.getCommitted(), poolTag);
  }

  // Non-heap usage JMX metric sums up Metaspace and Compressed Class Space, but in fact CCS is included in Metaspace
  private void sendNonHeapPoolMemoryUsageAdjusted(MemoryUsage poolUsage, MemoryUsage ccsUsage, Tag tag) {
    statsDSender.sendGauge(nonHeapUsedMetricName, poolUsage.getUsed() - ccsUsage.getUsed(), tag);
    statsDSender.sendGauge(nonHeapCommitedMetricName, poolUsage.getCommitted() - ccsUsage.getCommitted(), tag);
    statsDSender.sendGauge(nonHeapMaxMetricName, Math.max(-1, poolUsage.getMax() - ccsUsage.getMax()), tag);
  }

  private void sendBufferPoolsUsage() {
    for (BufferPoolMXBean bufferPool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
      Tag poolTag = new Tag("pool", bufferPool.getName());
      statsDSender.sendGauge(bufferPoolUsedMetricName, bufferPool.getMemoryUsed(), poolTag);
      statsDSender.sendGauge(bufferPoolCapacityMetricName, bufferPool.getTotalCapacity(), poolTag);
    }
  }

  private static String getFullMetricName(String serviceName, String metricName) {
    return serviceName + ".jvm." + metricName;
  }
}
