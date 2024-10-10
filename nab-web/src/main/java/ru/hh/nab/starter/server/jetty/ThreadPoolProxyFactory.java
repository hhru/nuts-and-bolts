package ru.hh.nab.starter.server.jetty;

import java.lang.reflect.Proxy;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.springframework.util.ClassUtils;
import ru.hh.nab.metrics.StatsDSender;

public class ThreadPoolProxyFactory {

  private final String serviceName;
  private final StatsDSender statsDSender;

  public ThreadPoolProxyFactory(String serviceName, StatsDSender statsDSender) {
    this.serviceName = serviceName;
    this.statsDSender = statsDSender;
  }

  public ThreadPool create(ThreadPool threadPool) {
    if (threadPool instanceof QueuedThreadPool queuedThreadPool) {
      return (ThreadPool) Proxy.newProxyInstance(
          queuedThreadPool.getClass().getClassLoader(),
          ClassUtils.getAllInterfaces(queuedThreadPool),
          new QueuedThreadPoolInvocationHandler(queuedThreadPool, serviceName, statsDSender)
      );
    } else {
      throw new IllegalArgumentException("ThreadPool of type QueuedThreadPool is expected");
    }
  }
}
