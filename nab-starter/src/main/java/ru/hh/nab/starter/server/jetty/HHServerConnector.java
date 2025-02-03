package ru.hh.nab.starter.server.jetty;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.SelectorManager;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.metrics.TaggedSender;

/**
 * ServerConnector that:<br/>
 * - immediately closes new incoming connections if there is no idle thread in the main thread pool;<br/>
 * - waits for current requests to end before completing shutdown;<br/>
 */
public final class HHServerConnector extends ServerConnector {
  public static final String LOW_ON_THREADS_METRIC_NAME = "service.low.on.threads";
  private final TaggedSender statsDSender;
  private static final Logger logger = LoggerFactory.getLogger(HHServerConnector.class);

  public HHServerConnector(@Name("server") Server server, TaggedSender statsDSender) {
    super(server);
    this.statsDSender = statsDSender;
  }

  public HHServerConnector(
      @Name("server") Server server,
      @Name("acceptors") int acceptors,
      @Name("selectors") int selectors,
      TaggedSender statsDSender
  ) {
    super(server, acceptors, selectors);
    this.statsDSender = statsDSender;
  }

  public HHServerConnector(
      @Name("server") Server server,
      @Name("acceptors") int acceptors,
      @Name("selectors") int selectors,
      TaggedSender statsDSender,
      @Name("factories") ConnectionFactory... factories
  ) {
    super(server, acceptors, selectors, factories);
    this.statsDSender = statsDSender;
  }

  public HHServerConnector(
      @Name("server") Server server,
      TaggedSender statsDSender,
      @Name("factories") ConnectionFactory... factories
  ) {
    super(server, factories);
    this.statsDSender = statsDSender;
  }

  public HHServerConnector(
      @Name("server") Server server,
      @Name("sslContextFactory") SslContextFactory.Server sslContextFactory,
      TaggedSender statsDSender
  ) {
    super(server, sslContextFactory);
    this.statsDSender = statsDSender;
  }

  public HHServerConnector(
      @Name("server") Server server,
      @Name("acceptors") int acceptors,
      @Name("selectors") int selectors,
      @Name("sslContextFactory") SslContextFactory.Server sslContextFactory,
      TaggedSender statsDSender
  ) {
    super(server, acceptors, selectors, sslContextFactory);
    this.statsDSender = statsDSender;
  }

  public HHServerConnector(
      @Name("server") Server server,
      @Name("sslContextFactory") SslContextFactory.Server sslContextFactory,
      TaggedSender statsDSender,
      @Name("factories") ConnectionFactory... factories
  ) {
    super(server, sslContextFactory, factories);
    this.statsDSender = statsDSender;
  }

  public HHServerConnector(
      @Name("server") Server server,
      @Name("executor") Executor executor,
      @Name("scheduler") Scheduler scheduler,
      @Name("bufferPool") ByteBufferPool bufferPool,
      @Name("acceptors") int acceptors,
      @Name("selectors") int selectors,
      TaggedSender statsDSender,
      @Name("factories") ConnectionFactory... factories
  ) {
    super(server, executor, scheduler, bufferPool, acceptors, selectors, factories);
    this.statsDSender = statsDSender;
  }

  @Override
  protected SelectorManager newSelectorManager(Executor executor, Scheduler scheduler, int selectors) {
    return new FailFastServerConnectorManager(executor, scheduler, selectors);
  }

  @Override
  public CompletableFuture<Void> shutdown() {
    super.shutdown();

    CompletableFuture<Void> shutdownFuture = new CompletableFuture<>();
    new ChannelsReadyChecker(shutdownFuture, this::getConnectedEndPoints, getScheduler()).run();
    return shutdownFuture;
  }

  private class FailFastServerConnectorManager extends ServerConnectorManager {

    FailFastServerConnectorManager(Executor executor, Scheduler scheduler, int selectors) {
      super(executor, scheduler, selectors);
    }

    @Override
    public void accept(SelectableChannel channel) {
      Executor executor = getExecutor();
      if (executor instanceof QueuedThreadPool) {
        QueuedThreadPool queuedThreadPool = (QueuedThreadPool) executor;
        if (queuedThreadPool.isLowOnThreads()) {
          statsDSender.sendCount(LOW_ON_THREADS_METRIC_NAME, 1);
          logger.debug("low on threads, closing accepted socket");
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

