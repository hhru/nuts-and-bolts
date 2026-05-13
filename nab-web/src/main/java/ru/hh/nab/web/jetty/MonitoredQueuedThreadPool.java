package ru.hh.nab.web.jetty;

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
  private final Max totalThreads = new Max(0);
  private final Max maxThreads = new Max(0);

  public MonitoredQueuedThreadPool(
      int maxThreads,
      int minThreads,
      int idleTimeout,
      BlockingQueue<Runnable> queue,
      String poolName,
      StatsDSender statsDSender
  ) {
    super(maxThreads, minThreads, idleTimeout, -1, queue, null);
    setName("qtp_" + poolName + "_" + hashCode());

    var sender = new TaggedSender(statsDSender, Set.of(new Tag("pool", poolName)));

    statsDSender.sendPeriodically(() -> {
      // Include current pool state in max so that load is reported correctly when no new jobs
      // are submitted during the interval (e.g. long-running jobs keep the pool busy).
      updatePoolMetrics();

      sender.sendMax("jetty.threadPool.queueSize", this.queueSize);
      sender.sendMax("jetty.threadPool.busyThreads", this.busyThreads);
      sender.sendMax("jetty.threadPool.totalThreads", this.totalThreads);
      sender.sendMax("jetty.threadPool.maxThreads", this.maxThreads);
    });
  }

  @Override
  public void execute(Runnable job) {
    updatePoolMetrics();

    super.execute(job);
  }

  private void updatePoolMetrics() {
    queueSize.save(getQueueSize());
    busyThreads.save(getBusyThreads());
    totalThreads.save(getThreads());
    maxThreads.save(getMaxThreads());
  }
}
