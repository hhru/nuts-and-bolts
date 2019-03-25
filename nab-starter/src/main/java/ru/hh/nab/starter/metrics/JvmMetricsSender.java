package ru.hh.nab.starter.metrics;

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
  private final String heapUsedMetric;
  private final String heapMaxMetric;
  private final String heapCommitedMetric;
  private final String nonHeapUsedMetric;
  private final String nonHeapMaxMetric;
  private final String nonHeapCommitedMetric;
  private final Tag totalPoolTag;

  private JvmMetricsSender(StatsDSender statsDSender, String serviceName) {
    this.statsDSender = statsDSender;
    this.memoryMXBean = ManagementFactory.getMemoryMXBean();
    this.heapUsedMetric = getMetricName(serviceName, "heap.used");
    this.heapMaxMetric = getMetricName(serviceName, "heap.max");
    this.heapCommitedMetric = getMetricName(serviceName, "heap.commited");
    this.nonHeapUsedMetric = getMetricName(serviceName, "nonHeap.used");
    this.nonHeapMaxMetric = getMetricName(serviceName, "nonHeap.max");
    this.nonHeapCommitedMetric = getMetricName(serviceName, "nonHeap.commited");
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
  }

  private void sendHeapMemoryUsage(MemoryUsage memoryUsage, Tag tag) {
    statsDSender.sendGauge(heapUsedMetric, memoryUsage.getUsed(), tag);
    statsDSender.sendGauge(heapMaxMetric, memoryUsage.getMax(), tag);
    statsDSender.sendGauge(heapCommitedMetric, memoryUsage.getCommitted(), tag);
  }

  private void sendNonHeapMemoryUsage(MemoryUsage memoryUsage, Tag tag) {
    statsDSender.sendGauge(nonHeapUsedMetric, memoryUsage.getUsed(), tag);
    statsDSender.sendGauge(nonHeapMaxMetric, memoryUsage.getMax(), tag);
    statsDSender.sendGauge(nonHeapCommitedMetric, memoryUsage.getCommitted(), tag);
  }

  private static String getMetricName(String serviceName, String metricName) {
    return serviceName + ".jvm." + metricName;
  }
}
