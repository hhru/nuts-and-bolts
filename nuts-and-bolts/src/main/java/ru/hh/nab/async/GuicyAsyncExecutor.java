package ru.hh.nab.async;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Injector;
import com.google.inject.Key;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.health.monitoring.TimingsLogger;
import ru.hh.nab.hibernate.Transactional;
import ru.hh.nab.hibernate.TxInterceptor;
import ru.hh.nab.scopes.RequestScope;

public class GuicyAsyncExecutor {
  public static final ThreadLocal<Boolean> killThisThread =
    new ThreadLocal<Boolean>() {
      @Override
      protected Boolean initialValue() {
        return false;
      }
    };

  private final Executor executor;
  private final Logger LOG = LoggerFactory.getLogger(GuicyAsyncExecutor.class);
  private final Injector inj;

  /** period in seconds, frequency of getting metrics and writing them in log file. 0 - means that monitoring is disabled */
  private final int monitoringPeriod;

  private final AtomicLong lastLogTime = new AtomicLong(DateTimeUtils.currentTimeMillis());

  public GuicyAsyncExecutor(Injector inj, String name, int threads) {
    this(inj, name, threads, Integer.MAX_VALUE, 0);
  }

  public GuicyAsyncExecutor(Injector inj, String name, int threads, int maxQueueSize, int monitoringPeriod) {
    ThreadFactory tf = new ThreadFactoryBuilder().setNameFormat(name + "-%d").build();
    this.inj = inj;
    this.executor = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(maxQueueSize), tf);
    this.monitoringPeriod = monitoringPeriod;
  }

  /** Method writes in log file current metrics of async executor if enabled */
  private void logExecutorMetrics() {
    if (monitoringPeriod <= 0 || !LOG.isDebugEnabled()) {
      return;
    }

    long current = DateTimeUtils.currentTimeMillis();
    long last = lastLogTime.get();

    if (current - last > monitoringPeriod && lastLogTime.compareAndSet(last, current)) {
      ThreadPoolExecutor threadPool = (ThreadPoolExecutor) executor;
      LOG.debug(
        "Current size of async executor queue = {}, current active thread count = {}, total thread count = {}",
        new String[] {
          Integer.toString(threadPool.getQueue().size()), Integer.toString(threadPool.getActiveCount()),
          Integer.toString(threadPool.getMaximumPoolSize())
        });
    }
  }

  public <T> Async<T> asyncWithTransferredRequestScope(Callable<T> body) {
    return new DeferredAsync<T>(body, RequestScope.currentClosure(), RequestScope.currentTimingsLogger());
  }

  static void killThisThreadAfterExecution() {
    killThisThread.set(true);
  }

  private class DeferredAsync<T> extends Async<T> {
    private final Callable<T> body;
    private final RequestScope.RequestScopeClosure requestScopeClosure;
    private final TimingsLogger timingsLogger;

    public DeferredAsync(Callable<T> body, RequestScope.RequestScopeClosure requestScopeClosure, TimingsLogger timingsLogger) {
      this.body = body;
      this.requestScopeClosure = requestScopeClosure;
      this.timingsLogger = timingsLogger;
    }

    @Override
    protected void runExposed(final Callback<T> onSuccess, final Callback<Throwable> onError) throws Exception {
      timingsLogger.probe("async-submission");
      logExecutorMetrics();
      try {
        executor.execute(
          new Runnable() {
            @Override
            public void run() {
              try {
                requestScopeClosure.enter();
                timingsLogger.probe("async-execution");
                final Callable<T> injCallable =
                  new Callable<T>() {
                    @Override
                    public T call() throws Exception {
                      inj.injectMembers(body);
                      return body.call();
                    }
                  };

                Callable<T> callable =
                  new Callable<T>() {
                    @Override
                    public T call() throws Exception {
                      try {
                        Transactional ann = body.getClass().getMethod("call").getAnnotation(Transactional.class);
                        if (ann == null) {
                          return injCallable.call();
                        }
                        TxInterceptor interceptor = inj.getInstance(Key.get(TxInterceptor.class, ann.value()));
                        return interceptor.invoke(ann, injCallable);
                      } finally {
                        timingsLogger.probe("async-finish");
                      }
                    }
                  };
                onSuccess.call(callable.call());
              } catch (Throwable e) {
                timingsLogger.setErrorState();
                try {
                  onError.call(e);
                } catch (Throwable ee) {
                  LOG.error("Exception in error handler", ee);
                  LOG.error("Original exception was", e);
                }
              } finally {
                requestScopeClosure.leave();
              }
            }
          });
      } catch (RejectedExecutionException ree) {
        onError.call(ree);
      }
      if (GuicyAsyncExecutor.killThisThread.get()) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
