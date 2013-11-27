package ru.hh.nab.async;

import com.google.inject.Injector;
import com.google.inject.Key;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.hibernate.Transactional;
import ru.hh.nab.hibernate.TxInterceptor;

public class OutOfScopeDeferredAsync<T> extends Async<T> {
  private final Logger LOG = LoggerFactory.getLogger(OutOfScopeDeferredAsync.class);

  private final Callable<T> body;

  private final Executor executor;

  private final Injector inj;

  public OutOfScopeDeferredAsync(final Executor executor, final Injector inj, final Callable<T> body) {
    this.body = body;
    this.executor = executor;
    this.inj = inj;
  }

  @Override
  protected void runExposed(final Callback<T> onSuccess, final Callback<Throwable> onError) throws Exception {
    try {
      executor.execute(
        new Runnable() {
          @Override
          public void run() {
            try {
              final Callable<T> injCallable =
                new Callable<T>() {
                  @Override
                  public T call() throws Exception {
                    inj.injectMembers(body);
                    return body.call();
                  }
                };

              final Callable<T> callable =
                new Callable<T>() {
                  @Override
                  public T call() throws Exception {
                    final Transactional ann = body.getClass().getMethod("call").getAnnotation(Transactional.class);
                    if (ann == null) {
                      return injCallable.call();
                    }
                    final TxInterceptor interceptor = inj.getInstance(Key.get(TxInterceptor.class, ann.value()));
                    return interceptor.invoke(ann, injCallable);
                  }
                };
              onSuccess.call(callable.call());
            } catch (Throwable e) {
              try {
                onError.call(e);
              } catch (Throwable ee) {
                LOG.error("Exception in error handler", ee);
                LOG.error("Original exception was", e);
              }
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
