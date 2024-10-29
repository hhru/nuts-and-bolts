package ru.hh.nab.web.jetty;

import org.eclipse.jetty.util.BlockingArrayQueue;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.starter.server.jetty.MonitoredQueuedThreadPool;

public class MonitoredQueuedThreadPoolFactory {

  private final String serviceName;
  private final StatsDSender statsDSender;

  public MonitoredQueuedThreadPoolFactory(String serviceName, StatsDSender statsDSender) {
    this.serviceName = serviceName;
    this.statsDSender = statsDSender;
  }

  public MonitoredQueuedThreadPool create(ServerProperties serverProperties) {
    ServerProperties.Jetty.Threads jettyThreadsProperties = serverProperties.getJetty().getThreads();
    return new MonitoredQueuedThreadPool(
        jettyThreadsProperties.getMax(),
        jettyThreadsProperties.getMin(),
        (int) jettyThreadsProperties.getIdleTimeout().toMillis(),
        new BlockingArrayQueue<>(jettyThreadsProperties.getMaxQueueCapacity()),
        serviceName,
        statsDSender
    );
  }
}
