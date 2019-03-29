package ru.hh.nab.starter.metrics;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.Tag;

public class JvmMetricsSender {
  private static final String COMPRESSED_CLASS_SPACE_POOL = "Compressed Class Space";
  private static final String METASPACE_POOL = "Metaspace";

  private static final Set<String> CODE_CACHE_POOLS = ManagementFactory.getMemoryManagerMXBeans().stream()
    .filter(mm -> mm.getName().equals("CodeCacheManager"))
    .findAny()
    .map(MemoryManagerMXBean::getMemoryPoolNames)
    .map(Set::of)
    .orElseGet(Set::of);

  private static final Tag TOTAL_TAG = new Tag("pool", "total");
  private static final Logger LOGGER = LoggerFactory.getLogger(JvmMetricsSender.class);

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

  private boolean codeCacheSizeMismatchReported = false;
  private boolean metaspaceSizeMismatchReported = false;

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
    long compressedClassSpaceMax = 0, metaspaceMax = 0, codeCacheMax = 0;
    for (MemoryPoolMXBean memoryPool : ManagementFactory.getMemoryPoolMXBeans()) {
      String poolName = memoryPool.getName();
      MemoryUsage memoryUsage = memoryPool.getUsage();

      if (memoryPool.getType() == MemoryType.HEAP) {
        sendHeapMemoryPoolUsage(memoryUsage, new Tag("pool", poolName));
      } else {
        sendNonHeapMemoryPoolUsage(memoryUsage, new Tag("pool", poolName));

        if (poolName.equals(COMPRESSED_CLASS_SPACE_POOL)) {
          compressedClassSpaceMax = memoryUsage.getMax();
        } else if (poolName.equals(METASPACE_POOL)) {
          metaspaceMax = memoryUsage.getMax();
        } else if (CODE_CACHE_POOLS.contains(memoryPool.getName())) {
          codeCacheMax += memoryUsage.getMax();
        }
      }
    }

    sendHeapMemoryUsage();
    sendNonHeapMemoryUsage(compressedClassSpaceMax);

    sendBufferPoolsUsage();

    long loadedClassesCount = ManagementFactory.getClassLoadingMXBean().getLoadedClassCount();
    statsDSender.sendGauge(loadedClassesCountMetricName, loadedClassesCount);
    statsDSender.sendGauge(threadCountMetricName, ManagementFactory.getThreadMXBean().getThreadCount());

    if (!codeCacheSizeMismatchReported) {
      logCodeCacheSizeMismatch(codeCacheMax, loadedClassesCount);
      codeCacheSizeMismatchReported = true;
    }

    if (!metaspaceSizeMismatchReported) {
      logMetaspaceSizeMismatch(metaspaceMax, loadedClassesCount);
      metaspaceSizeMismatchReported = true;
    }
  }

  private void sendHeapMemoryUsage() {
    sendHeapMemoryPoolUsage(memoryMXBean.getHeapMemoryUsage(), TOTAL_TAG);
  }

  private void sendNonHeapMemoryUsage(long compressedClassSpaceMax) {
    MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
    statsDSender.sendGauge(nonHeapUsedMetricName, nonHeapMemoryUsage.getUsed(), TOTAL_TAG);
    statsDSender.sendGauge(nonHeapCommitedMetricName, nonHeapMemoryUsage.getCommitted(), TOTAL_TAG);

    // Max non-heap usage sums up maximums for Metaspace and Compressed Class Space, but in fact CCS is included in Metaspace
    statsDSender.sendGauge(nonHeapMaxMetricName, Math.max(-1, nonHeapMemoryUsage.getMax() - compressedClassSpaceMax), TOTAL_TAG);
  }

  private void sendHeapMemoryPoolUsage(MemoryUsage memoryUsage, Tag poolTag) {
    statsDSender.sendGauge(heapUsedMetricName, memoryUsage.getUsed(), poolTag);
    statsDSender.sendGauge(heapMaxMetricName, memoryUsage.getMax(), poolTag);
    statsDSender.sendGauge(heapCommitedMetricName, memoryUsage.getCommitted(), poolTag);
  }

  private void sendNonHeapMemoryPoolUsage(MemoryUsage memoryUsage, Tag poolTag) {
    statsDSender.sendGauge(nonHeapUsedMetricName, memoryUsage.getUsed(), poolTag);
    statsDSender.sendGauge(nonHeapMaxMetricName, memoryUsage.getMax(), poolTag);
    statsDSender.sendGauge(nonHeapCommitedMetricName, memoryUsage.getCommitted(), poolTag);
  }

  private void sendBufferPoolsUsage() {
    for (BufferPoolMXBean bufferPool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
      Tag poolTag = new Tag("pool", bufferPool.getName());
      statsDSender.sendGauge(bufferPoolUsedMetricName, bufferPool.getMemoryUsed(), poolTag);
      statsDSender.sendGauge(bufferPoolCapacityMetricName, bufferPool.getTotalCapacity(), poolTag);
    }
  }

  private static void logCodeCacheSizeMismatch(long codeCacheMax, long loadedClassesCount) {
    long recommendedCodeCacheSize = getRecommendedCodeCacheSize(loadedClassesCount);
    if (codeCacheMax < recommendedCodeCacheSize) {
      LOGGER.error(
        "CodeCache limit ({}) is less than recommended ({}), consider increasing -XX:ReservedCodeCacheSize",
        codeCacheMax, recommendedCodeCacheSize
      );
    } else if (codeCacheMax > recommendedCodeCacheSize * 2) {
      LOGGER.error(
        "CodeCache limit ({}) is much more than recommended ({}), consider decreasing -XX:ReservedCodeCacheSize",
        codeCacheMax, recommendedCodeCacheSize
      );
    }
  }

  private static void logMetaspaceSizeMismatch(long metaspaceMax, long loadedClassesCount) {
    long recommendedMetaspaceSize = getRecommendedMetaspaceSize(loadedClassesCount);
    if (metaspaceMax < 0) {
      LOGGER.error(
        "Metaspace is unlimited (recommended: {}), consider setting -XX:MaxMetaspaceSize", recommendedMetaspaceSize
      );
    } else if (metaspaceMax < recommendedMetaspaceSize) {
      LOGGER.error(
        "Metaspace limit ({}) is less than recommended ({}), consider increasing -XX:MaxMetaspaceSize",
        metaspaceMax, recommendedMetaspaceSize
      );
    } else if (metaspaceMax > recommendedMetaspaceSize * 2) {
      LOGGER.error(
        "Metaspace limit ({}) is much more than recommended ({}), consider increasing -XX:MaxMetaspaceSize",
        metaspaceMax, recommendedMetaspaceSize
      );
    }
  }

  // Inspired by https://github.com/cloudfoundry/java-buildpack-memory-calculator

  private static long getRecommendedMetaspaceSize(long loadedClassesCount) {
    return (loadedClassesCount * 5800) + 14_000_000;
  }

  private static long getRecommendedCodeCacheSize(long loadedClassesCount) {
    return (loadedClassesCount * 1500) + 5_000_000;
  }

  private static String getFullMetricName(String serviceName, String metricName) {
    return serviceName + ".jvm." + metricName;
  }
}
