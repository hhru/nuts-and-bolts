package ru.hh.nab.async;

import com.google.common.base.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Async<T> {

  private final static Logger log = LoggerFactory.getLogger(Async.class);

  protected abstract void runExposed(Callback<T> onSuccess, Callback<Throwable> onError) throws Exception;

  public void run(Callback<T> onSuccess, Callback<Throwable> onError) {
    try {
      runExposed(onSuccess, onError);
    } catch (Exception e) {
      try {
        onError.call(e);
      } catch (Exception e1) {
        log.error("Error while handling exception", e1);
      }
    }
  }

  public <F> Async<F> then(final Function<T, Async<F>> fn) {
    return new Async<F>() {
      @Override
      public void runExposed(final Callback<F> onSuccess, final Callback<Throwable> onError) {
        Async.this.run(
            new Callback<T>() {
              @Override
              public void call(T result) {
                fn.apply(result).run(onSuccess, onError);
              }
            },
            onError
        );
      }
    };
  }

  public static Async<Void> startAsync() {
    return new Async<Void>() {
      @Override
      protected void runExposed(Callback<Void> onSuccess, Callback<Throwable> onError) throws Exception {
        onSuccess.call(null);
      }
    };
  }
}
