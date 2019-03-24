package ru.hh.nab.starter.server.jetty;

import org.eclipse.jetty.util.thread.QueuedThreadPool;
import ru.hh.nab.metrics.Max;
import ru.hh.nab.metrics.StatsDSender;

import java.util.concurrent.BlockingQueue;

public class MonitoredQueuedThreadPool extends QueuedThreadPool {
  private final Max queueSize = new Max(0);
  private final Max busyThreads = new Max(0);
  private final Max idleThreads = new Max(0);
  private final Max totalThreads = new Max(0);

  public MonitoredQueuedThreadPool(int maxThreads, int minThreads, int idleTimeout, BlockingQueue<Runnable> queue,
                                   String serviceName, StatsDSender statsDSender) {
    super(maxThreads, minThreads, idleTimeout, -1, queue, null);

    String queueSizeMetricName = getFullMetricName(serviceName, "queueSize");
    String busyThreadsMetricName = getFullMetricName(serviceName, "busyThreads");
    String idleThreadsMetricName = getFullMetricName(serviceName, "idleThreads");
    String totalThreadsMetricName = getFullMetricName(serviceName, "totalThreads");

    statsDSender.sendPeriodically(() -> {
      statsDSender.sendMax(queueSizeMetricName, queueSize);
      statsDSender.sendMax(busyThreadsMetricName, busyThreads);
      statsDSender.sendMax(idleThreadsMetricName, idleThreads);
      statsDSender.sendMax(totalThreadsMetricName, totalThreads);
    });
  }

  @Override
  public void execute(Runnable job) {
    queueSize.save(getQueueSize());
    busyThreads.save(getBusyThreads());
    idleThreads.save(getIdleThreads());
    totalThreads.save(getThreads());

    super.execute(job);
  }

  private String getFullMetricName(String serviceName, String shortMetricName) {
    return serviceName + ".jetty.threadPool." + shortMetricName;
  }
}
