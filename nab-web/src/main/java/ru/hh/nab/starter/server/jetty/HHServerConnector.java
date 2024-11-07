package ru.hh.nab.starter.server.jetty;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.util.Set;
import java.util.concurrent.Executor;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.SelectorManager;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.Scheduler;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.Tag;
import static ru.hh.nab.metrics.Tag.APP_TAG_NAME;
import ru.hh.nab.metrics.TaggedSender;

/**
 * ServerConnector that immediately closes new incoming connections if there is no idle thread in the main thread pool
 */
public final class HHServerConnector extends ServerConnector {
  public static final String LOW_ON_THREADS_METRIC_NAME = "service.low.on.threads";
  private final TaggedSender statsDSender;
  private static final Logger logger = LoggerFactory.getLogger(HHServerConnector.class);

  public HHServerConnector(Server server, int acceptors, int selectors, StatsDSender statsDSender, String serviceName) {
    super(server, acceptors, selectors);
    this.statsDSender = createTaggedSender(statsDSender, serviceName);
  }

  public HHServerConnector(
      Server server,
      Executor executor,
      Scheduler scheduler,
      ByteBufferPool bufferPool,
      int acceptors,
      int selectors,
      StatsDSender statsDSender,
      String serviceName,
      ConnectionFactory... factories
  ) {
    super(server, executor, scheduler, bufferPool, acceptors, selectors, factories);
    this.statsDSender = createTaggedSender(statsDSender, serviceName);
  }

  @Override
  protected SelectorManager newSelectorManager(Executor executor, Scheduler scheduler, int selectors) {
    return new FailFastServerConnectorManager(executor, scheduler, selectors);
  }

  private TaggedSender createTaggedSender(StatsDSender statsDSender, String serviceName) {
    return new TaggedSender(statsDSender, Set.of(new Tag(APP_TAG_NAME, serviceName)));
  }

  private class FailFastServerConnectorManager extends ServerConnectorManager {

    FailFastServerConnectorManager(Executor executor, Scheduler scheduler, int selectors) {
      super(executor, scheduler, selectors);
    }

    @Override
    public void accept(SelectableChannel channel) {
      Executor executor = getExecutor();
      if (executor instanceof ThreadPool threadPool) {
        if (threadPool.isLowOnThreads()) {
          statsDSender.sendCount(LOW_ON_THREADS_METRIC_NAME, 1);
          logger.warn("low on threads, closing accepted socket");
          try {
            channel.close();
          } catch (IOException e) {
            logger.warn("failed to close socket, leaving socket as is", e);
          }
          return;
        }
      }
      super.accept(channel);
    }
  }
}

