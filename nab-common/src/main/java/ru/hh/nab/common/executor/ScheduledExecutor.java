package ru.hh.nab.common.executor;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduled executor that catches exceptions in order to: <br/>
 * - avoid stop execution of scheduleAtFixedRate and scheduleWithFixedDelay<br/>
 * - log them <br/>
 **/
public class ScheduledExecutor implements ScheduledExecutorService {

  private static final Logger logger = LoggerFactory.getLogger(ScheduledExecutor.class);

  private final ScheduledExecutorService scheduledExecutorService;

  public ScheduledExecutor() {
    this("hh_scheduled_executor");
  }

  public ScheduledExecutor(String threadName) {
    ThreadFactory threadFactory = runnable -> {
      Thread thread = new Thread(runnable, threadName);
      thread.setDaemon(true);
      return thread;
    };

    // One thread is dangerous, because some long running task can block it. Let's use a little bit more.
    this.scheduledExecutorService = new ScheduledThreadPoolExecutor(2, threadFactory);
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    return scheduledExecutorService.schedule(command, delay, unit);
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    return scheduledExecutorService.schedule(callable, delay, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    Runnable wrappedCommand = wrap(command);
    return scheduledExecutorService.scheduleAtFixedRate(wrappedCommand, initialDelay, period, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    Runnable wrappedCommand = wrap(command);
    return scheduledExecutorService.scheduleWithFixedDelay(wrappedCommand, initialDelay, delay, unit);
  }

  @Override
  public void shutdown() {
    scheduledExecutorService.shutdown();
  }

  @Override
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
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return scheduledExecutorService.awaitTermination(timeout, unit);
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return scheduledExecutorService.submit(task);
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    return scheduledExecutorService.submit(task, result);
  }

  @Override
  public Future<?> submit(Runnable task) {
    return scheduledExecutorService.submit(task);
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return scheduledExecutorService.invokeAll(tasks);
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
                                       TimeUnit unit) throws InterruptedException {
    return scheduledExecutorService.invokeAll(tasks, timeout, unit);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    return scheduledExecutorService.invokeAny(tasks);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
          throws InterruptedException, ExecutionException, TimeoutException {
    return scheduledExecutorService.invokeAny(tasks, timeout, unit);
  }

  @Override
  public void execute(Runnable command) {
    scheduledExecutorService.execute(command);
  }

  private static Runnable wrap(Runnable runnable) {
    return () -> {
      try {
        runnable.run();
      } catch (RuntimeException e) {
        logger.error("Task execution failed", e);
      } catch (Error e) {
        logger.error("Task execution failed and will not be rescheduled", e);
        throw e;
      }
    };
  }
}
