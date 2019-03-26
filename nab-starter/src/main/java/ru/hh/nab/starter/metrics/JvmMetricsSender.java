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
  private final Tag totalPoolTag;

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
    this.totalPoolTag = new Tag("pool", "total");
  }

  public static void create(StatsDSender statsDSender, String serviceName) {
    JvmMetricsSender jvmMetricsSender = new JvmMetricsSender(statsDSender, serviceName);
    statsDSender.sendPeriodically(jvmMetricsSender::sendJvmMetrics);
  }

  private void sendJvmMetrics() {
    sendHeapMemoryUsage(memoryMXBean.getHeapMemoryUsage(), totalPoolTag);
    sendNonHeapMemoryUsage(memoryMXBean.getNonHeapMemoryUsage(), totalPoolTag);

    for (MemoryPoolMXBean memoryPool : ManagementFactory.getMemoryPoolMXBeans()) {
      Tag poolTag = new Tag("pool", memoryPool.getName());

      if (memoryPool.getType() == MemoryType.HEAP) {
        sendHeapMemoryUsage(memoryPool.getUsage(), poolTag);
      } else {
        sendNonHeapMemoryUsage(memoryPool.getUsage(), poolTag);
      }
    }

    for (BufferPoolMXBean bufferPool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
      Tag poolTag = new Tag("pool", bufferPool.getName());
      statsDSender.sendGauge(bufferPoolUsedMetricName, bufferPool.getMemoryUsed(), poolTag);
      statsDSender.sendGauge(bufferPoolCapacityMetricName, bufferPool.getTotalCapacity(), poolTag);
    }
  }

  private void sendHeapMemoryUsage(MemoryUsage memoryUsage, Tag tag) {
    statsDSender.sendGauge(heapUsedMetricName, memoryUsage.getUsed(), tag);
    statsDSender.sendGauge(heapMaxMetricName, memoryUsage.getMax(), tag);
    statsDSender.sendGauge(heapCommitedMetricName, memoryUsage.getCommitted(), tag);
  }

  private void sendNonHeapMemoryUsage(MemoryUsage memoryUsage, Tag tag) {
    statsDSender.sendGauge(nonHeapUsedMetricName, memoryUsage.getUsed(), tag);
    statsDSender.sendGauge(nonHeapMaxMetricName, memoryUsage.getMax(), tag);
    statsDSender.sendGauge(nonHeapCommitedMetricName, memoryUsage.getCommitted(), tag);
  }

  private static String getFullMetricName(String serviceName, String metricName) {
    return serviceName + ".jvm." + metricName;
  }
}
