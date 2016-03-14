package ru.hh.nab.async;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Async<T> {

  private final static Logger log = LoggerFactory.getLogger(Async.class);

  protected abstract void runExposed(Callback<T> onSuccess, Callback<Throwable> onError) throws Exception;

  public final void run(Callback<T> onSuccess, Callback<Throwable> onError) {
    try {
      runExposed(onSuccess, onError);
    } catch (Exception e) {
      try {
        onError.call(e);
      } catch (Exception e1) {
        log.error("Error while handling exception", e1);
        log.error("Original exception was", e);
      }
    }
  }

  public final T awaitChecked() throws Throwable {
    AtomicReference<T> ret = new AtomicReference<T>();
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> ex = new AtomicReference<Throwable>();
    run(Callbacks.storeAndCountDown(ret, latch),
            Callbacks.storeAndCountDown(ex, latch));
    latch.await();
    if (ex.get() != null) {
      throw ex.get();
    }
    return ret.get();
  }

  public final T await() {
    try {
      return awaitChecked();
    } catch (Throwable throwable) {
      if (throwable instanceof RuntimeException) {
        throw (RuntimeException)throwable;
      } else {
        throw new RuntimeException(throwable);
      }
    }
  }
}
