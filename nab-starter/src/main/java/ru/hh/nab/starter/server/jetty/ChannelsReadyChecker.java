package ru.hh.nab.starter.server.jetty;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.HttpChannelState;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.util.thread.Scheduler;

class ChannelsReadyChecker implements Runnable {

  private final CompletableFuture<Void> channelsReadyFuture;
  private final Supplier<Collection<EndPoint>> endPointsSupplier;
  private final Scheduler scheduler;

  ChannelsReadyChecker(
      CompletableFuture<Void> channelsReadyFuture,
      Supplier<Collection<EndPoint>> endPointsSupplier,
      Scheduler scheduler
  ) {
    this.channelsReadyFuture = channelsReadyFuture;
    this.endPointsSupplier = endPointsSupplier;
    this.scheduler = scheduler;
  }

  @Override
  public void run() {
    if (channelsReadyFuture.isCancelled()) {
      return;
    }
    try {
      if (allEndPointsReadyToBeClosed()) {
        channelsReadyFuture.complete(null);
      } else {
        scheduler.schedule(
            new ChannelsReadyChecker(channelsReadyFuture, endPointsSupplier, scheduler),
            10, TimeUnit.MILLISECONDS
        );
      }
    } catch (RuntimeException e) {
      channelsReadyFuture.completeExceptionally(e);
    }
  }

  private boolean allEndPointsReadyToBeClosed() {
    return endPointsSupplier.get().stream().allMatch(ChannelsReadyChecker::endPointReadyToBeClosed);
  }

  private static boolean endPointReadyToBeClosed(EndPoint endPoint) {
    Connection connection = endPoint.getConnection();
    if (!(connection instanceof HttpConnection)) {
      return true;
    }
    HttpConnection httpConnection = (HttpConnection) connection;
    HttpChannelState channelState = httpConnection.getHttpChannel().getState();
    HttpChannelState.State state = channelState.getState();
    return channelState.isResponseCompleted() || state == HttpChannelState.State.IDLE;
  }
}
