package ru.hh.nab.starter.server.jetty;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Set;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.springframework.util.ClassUtils;
import ru.hh.nab.metrics.Max;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.Tag;
import ru.hh.nab.metrics.TaggedSender;

public class QueuedThreadPoolInvocationHandler implements InvocationHandler {

  private static final String EXECUTE_METHOD = ClassUtils.getMethod(QueuedThreadPool.class, "execute", Runnable.class).getName();
  private static final String TRY_EXECUTE_METHOD = ClassUtils.getMethod(QueuedThreadPool.class, "tryExecute", Runnable.class).getName();

  private final Max queueSize = new Max(0);
  private final Max busyThreads = new Max(0);
  private final Max idleThreads = new Max(0);
  private final Max totalThreads = new Max(0);
  private final Max maxThreads = new Max(0);

  private final QueuedThreadPool threadPool;

  public QueuedThreadPoolInvocationHandler(QueuedThreadPool threadPool, String poolName, StatsDSender statsDSender) {
    this.threadPool = threadPool;

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
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (method.getName().equals(EXECUTE_METHOD) || method.getName().equals(TRY_EXECUTE_METHOD)) {
      queueSize.save(threadPool.getQueueSize());
      busyThreads.save(threadPool.getBusyThreads());
      idleThreads.save(threadPool.getIdleThreads());
      totalThreads.save(threadPool.getThreads());
      maxThreads.save(threadPool.getMaxThreads());
    }
    return method.invoke(threadPool, args);
  }
}
