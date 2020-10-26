package ru.hh.nab.starter.server.jetty;

import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.SelectorManager;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.LongAdder;

/**
 * ServerConnector that:<br/>
 * - immediately closes new incoming connections if there is no idle thread in the main thread pool;<br/>
 * - waits for current requests to end before completing shutdown;<br/>
 */
public final class HHServerConnector extends ServerConnector {

  private static final Logger logger = LoggerFactory.getLogger(HHServerConnector.class);
  private LongAdder requestStat = new LongAdder();

  public HHServerConnector(@Name("server") Server server) {
    super(server);
  }

  public HHServerConnector(@Name("server") Server server,
                           @Name("acceptors") int acceptors,
                           @Name("selectors") int selectors) {
    super(server, acceptors, selectors);
  }

  public HHServerConnector(@Name("server") Server server,
                           @Name("acceptors") int acceptors,
                           @Name("selectors") int selectors,
                           @Name("factories") ConnectionFactory... factories) {
    super(server, acceptors, selectors, factories);
  }

  public HHServerConnector(@Name("server") Server server,
                           @Name("factories") ConnectionFactory... factories) {
    super(server, factories);
  }

  public HHServerConnector(@Name("server") Server server,
                           @Name("sslContextFactory") SslContextFactory sslContextFactory) {
    super(server, sslContextFactory);
  }

  public HHServerConnector(@Name("server") Server server,
                           @Name("acceptors") int acceptors,
                           @Name("selectors") int selectors,
                           @Name("sslContextFactory") SslContextFactory sslContextFactory) {
    super(server, acceptors, selectors, sslContextFactory);
  }

  public HHServerConnector(@Name("server") Server server,
                           @Name("sslContextFactory") SslContextFactory sslContextFactory,
                           @Name("factories") ConnectionFactory... factories) {
    super(server, sslContextFactory, factories);
  }

  public HHServerConnector(@Name("server") Server server,
                           @Name("executor") Executor executor,
                           @Name("scheduler") Scheduler scheduler,
                           @Name("bufferPool") ByteBufferPool bufferPool,
                           @Name("acceptors") int acceptors,
                           @Name("selectors") int selectors,
                           @Name("factories") ConnectionFactory... factories) {
    super(server, executor, scheduler, bufferPool, acceptors, selectors, factories);
  }

  @Override
  protected SelectorManager newSelectorManager(Executor executor, Scheduler scheduler, int selectors) {
    return new FailFastServerConnectorManager(executor, scheduler, selectors);
  }

  private class FailFastServerConnectorManager extends ServerConnectorManager {

    FailFastServerConnectorManager(Executor executor, Scheduler scheduler, int selectors) {
      super(executor, scheduler, selectors);
    }

    @Override
    public void accept(SelectableChannel channel) {
      requestStat.increment();
      long sum = requestStat.sum();
      if (sum % 10000 == 0) {
        logger.warn("low on threads, closing accepted socket already for " + sum + " requests");
      }
      try {
        channel.close();
      } catch (IOException e) {
        logger.warn("failed to close socket, leaving socket as is", e);
      }
    }
  }

  @Override
  public Future<Void> shutdown() {
    super.shutdown();

    CompletableFuture<Void> shutdownFuture = new CompletableFuture<>();
    new ChannelsReadyChecker(shutdownFuture, this::getConnectedEndPoints, getScheduler()).run();
    return shutdownFuture;
  }

}

