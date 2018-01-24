package ru.hh.nab.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Scheduled executor that catches exceptions in order to: <br/>
 * - avoid stop execution of scheduleAtFixedRate and scheduleWithFixedDelay<br/>
 * - log them <br/>
 **/
public class HhScheduledExecutor implements ScheduledExecutorService {

  private static final Logger logger = LoggerFactory.getLogger(HhScheduledExecutor.class);

  private final ScheduledExecutorService scheduledExecutorService;

  public HhScheduledExecutor() {
    ThreadFactory threadFactory = runnable -> {
      Thread thread = new Thread(runnable, "hh_scheduled_executor");
      thread.setDaemon(true);
      return thread;
    };

    // One thread is dangerous, because some long running task can block it. Let's use a little bit more.
    this.scheduledExecutorService = new ScheduledThreadPoolExecutor(2, threadFactory);
  }

  @Override
  @Nonnull
  public ScheduledFuture<?> schedule(@Nonnull Runnable command, long delay, @Nonnull TimeUnit unit) {
    return scheduledExecutorService.schedule(command, delay, unit);
  }

  @Override
  @Nonnull
  public <V> ScheduledFuture<V> schedule(@Nonnull Callable<V> callable, long delay, @Nonnull TimeUnit unit) {
    return scheduledExecutorService.schedule(callable, delay, unit);
  }

  @Override
  @Nonnull
  public ScheduledFuture<?> scheduleAtFixedRate(@Nonnull Runnable command, long initialDelay, long period,
                                                @Nonnull TimeUnit unit) {
    Runnable wrappedCommand = wrap(command);
    return scheduledExecutorService.scheduleAtFixedRate(wrappedCommand, initialDelay, period, unit);
  }

  @Override
  @Nonnull
  public ScheduledFuture<?> scheduleWithFixedDelay(@Nonnull Runnable command, long initialDelay, long delay,
                                                   @Nonnull TimeUnit unit) {
    Runnable wrappedCommand = wrap(command);
    return scheduledExecutorService.scheduleWithFixedDelay(wrappedCommand, initialDelay, delay, unit);
  }

  @Override
  public void shutdown() {
    scheduledExecutorService.shutdown();
  }

  @Override
  @Nonnull
  public List<Runnable> shutdownNow() {
    return scheduledExecutorService.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return scheduledExecutorService.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return scheduledExecutorService.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
    return scheduledExecutorService.awaitTermination(timeout, unit);
  }

  @Override
  @Nonnull
  public <T> Future<T> submit(@Nonnull Callable<T> task) {
    return scheduledExecutorService.submit(task);
  }

  @Override
  @Nonnull
  public <T> Future<T> submit(@Nonnull Runnable task, T result) {
    return scheduledExecutorService.submit(task, result);
  }

  @Override
  @Nonnull
  public Future<?> submit(@Nonnull Runnable task) {
    return scheduledExecutorService.submit(task);
  }

  @Override
  @Nonnull
  public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return scheduledExecutorService.invokeAll(tasks);
  }

  @Override
  @Nonnull
  public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks, long timeout,
                                       @Nonnull TimeUnit unit) throws InterruptedException {
    return scheduledExecutorService.invokeAll(tasks, timeout, unit);
  }

  @Override
  @Nonnull
  public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    return scheduledExecutorService.invokeAny(tasks);
  }

  @Override
  @Nonnull
  public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit)
          throws InterruptedException, ExecutionException, TimeoutException {
    return scheduledExecutorService.invokeAny(tasks, timeout, unit);
  }

  @Override
  public void execute(@Nonnull Runnable command) {
    scheduledExecutorService.execute(command);
  }

  private static Runnable wrap(Runnable runnable) {
    return () -> {
      try {
        runnable.run();
      } catch (RuntimeException e) {
        logger.error("failed to run task: {}", e.toString(), e);
      }
    };
  }
}
