package ru.hh.nab.starter.server.jetty;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import ru.hh.nab.metrics.Max;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.Tag;
import ru.hh.nab.metrics.TaggedSender;

public class MonitoredQueuedThreadPool extends QueuedThreadPool {
  private final Max queueSize = new Max(0);
  private final Max busyThreads = new Max(0);
  private final Max idleThreads = new Max(0);
  private final Max totalThreads = new Max(0);
  private final Max maxThreads = new Max(0);

  public MonitoredQueuedThreadPool(int maxThreads, int minThreads, int idleTimeout, BlockingQueue<Runnable> queue,
                                   String poolName, StatsDSender statsDSender) {
    super(maxThreads, minThreads, idleTimeout, -1, queue, null);

    String queueSizeMetricName = "queueSize";
    String busyThreadsMetricName = "busyThreads";
    String idleThreadsMetricName = "idleThreads";
    String totalThreadsMetricName = "totalThreads";
    String maxThreadsMetricName = "maxThreads";
    var sender = new TaggedSender(statsDSender, Set.of(new Tag("pool", poolName)));

    statsDSender.sendPeriodically(() -> {
      sender.sendMax(queueSizeMetricName, queueSize);
      sender.sendMax(busyThreadsMetricName, busyThreads);
      sender.sendMax(idleThreadsMetricName, idleThreads);
      sender.sendMax(totalThreadsMetricName, totalThreads);
      sender.sendMax(maxThreadsMetricName, this.maxThreads);
    });
  }

  @Override
  public void execute(Runnable job) {
    queueSize.save(getQueueSize());
    busyThreads.save(getBusyThreads());
    idleThreads.save(getIdleThreads());
    totalThreads.save(getThreads());
    maxThreads.save(getMaxThreads());

    super.execute(job);
  }
}
