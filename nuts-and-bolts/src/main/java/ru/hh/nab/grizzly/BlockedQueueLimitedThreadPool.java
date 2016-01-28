package ru.hh.nab.grizzly;

import org.glassfish.grizzly.threadpool.FixedThreadPool;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;


final class BlockedQueueLimitedThreadPool extends FixedThreadPool {
  public static final int MAX_QUEUE_SIZE = 30000;
  private final Semaphore queuePermits;

  BlockedQueueLimitedThreadPool(ThreadPoolConfig config) {
    super(config);
    if (config.getQueueLimit() < 0 || config.getQueueLimit() > MAX_QUEUE_SIZE) {
      throw new IllegalArgumentException("0 < maxQueuedTasks < " + MAX_QUEUE_SIZE);
    }

    queuePermits = new Semaphore(config.getQueueLimit());
  }

  @Override
  public final void execute(Runnable command) {
    if (command == null) { // must nullcheck to ensure queuesize is valid
      throw new IllegalArgumentException("Runnable task is null");
    }

    if (!running) {
      throw new RejectedExecutionException("ThreadPool is not running");
    }

    for (;;) {
      try {
        queuePermits.acquire();

        // for unbounded queue tasks count may be greater than config.getQueueLimit()
        boolean added = workQueue.offer(command);
        if (!added) {
          queuePermits.release();
        } else {
          break;
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RejectedExecutionException("Thread was interrupted");
      }
    }

    onTaskQueued(command);
  }

  @Override
  protected final void beforeExecute(final Worker worker, final Thread t,
                                     final Runnable r) {
    super.beforeExecute(worker, t, r);
    queuePermits.release();
  }
}
