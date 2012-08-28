package ru.hh.nab.async;

import com.google.common.base.Function;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* IMPORTANT NOTE
 *
 *  1. The original purpose of async is to do certain tasks without
 *  blocking the original thread, so that this thread can be reused
 *  again by the calling code (assuming that it came from a thread
 *  pool).
 *
 *  1. Async is a very generic concept, but in our code it is
 *  almost always used in the context of Jersey/Grizzly calls
 *  with RequestScope.
 *
 *  This means that execution is very often continued in other
 *  threads, which by default do not have RequestScope initialized.
 *  This can (and is known to) cause all kinds of mess, unless
 *  the following simple rules are followed:
 *
 *   a) When writing functions which return Async<T> instances,
 *      assume that RequestScope is already entered.
 *
 *   b) when overriding runExposed() function, assume that
 *      RequestScope is already entered.
 *
 *   c) when creating Callbacks to be called from another
 *      thread (for example, by async http client), enter
 *      the scope in the callback before doing anything else,
 *      and leave in the finally clause.
 *
 *      CallbackWithRequestScope class can help with that.
 *
 *   d) Same for any code running in other threads, always enter
 *      RequestScope as soon as our code gets control of execution
 *      and leave in finally clause.
 *
 *      The only code which can be run without RequestScope is
 *      the 3rd party library code (i.e. async http client code)
 *      from the point where it starts and until the point
 *      where our callbacks are called.
 *
 *  Example:
 *
 *  <pre>
 *  protected void runExposed(Callback<String> onSuccess, Callback<Throwable> onError) throws Exception {
 *    final Callback<String> scopedOnSuccess = CallbackWithRequestScope.fromCallback(onSuccess);
 *    final Callback<Throwable> scopedOnError = CallbackWithRequestScope.fromCallback(onError);
 *    ...
 *    httpClient
 *      ...
 *      .execute(
 *        new AsyncCompletionHandler<Object>() {
 *
 *          @Override
 *          public void onThrowable(Throwable t) {
 *            try {
 *              scopedOnError.call(t);
 *            } catch (Exception e) {
 *              throw new RuntimeException(e);
 *            }
 *          }
 *
 *          @Override
 *          public Object onCompleted(Response response) throws Exception {
 *            ...
 *            if (result.isError())
 *              scopedOnError.call(new ErrorResult(result.getTextValue()));
 *            else
 *              scopedOnSuccess.call(result.getTextValue());
 *            return null;
 *          }
 *        }
 *      );
 *  }
 * </pre>
 *
 * In this example we do some results handling before calling
 * callbacks with request scope which is ok but is very good.
 *
 * We could also do this:
 *
 *  <pre>
 *  protected abstract static class RequestScopedAsyncCompletionHandler<T> extends AsyncCompletionHandler<T> {
 *   private final RequestScopeClosure requestScopeClosure;
 *
 *   public RequestScopedAsyncCompletionHandler() {
 *     this.requestScopeClosure = RequestScope.currentClosure();
 *   }
 *
 *   @Override
 *   public final void onThrowable(Throwable t) {
 *     try {
 *       requestScopeClosure.enter();
 *       onThrowableExposed(t);
 *     } finally {
 *       requestScopeClosure.leave();
 *     }
 *   }
 *
 *   @Override
 *   public final T onCompleted(Response response) throws Exception {
 *     try {
 *       requestScopeClosure.enter();
 *       return onCompletedExposed(response);
 *     } finally {
 *       requestScopeClosure.leave();
 *     }
 *   }
 *
 *   protected abstract void onThrowableExposed(Throwable t);
 *
 *   protected abstract T onCompletedExposed(Response response) throws Exception;
 * }
 *
 *  protected void runExposed(Callback<String> onSuccess, Callback<Throwable> onError) throws Exception {
 *    ...
 *    httpClient
 *      ...
 *      .execute(
 *        new RequestScopedAsyncCompletionHandler<Object>() {
 *
 *          @Override
 *          public void onThrowableExposed(Throwable t) {
 *            try {
 *              onError.call(t);
 *            } catch (Exception e) {
 *              throw new RuntimeException(e);
 *            }
 *          }
 *
 *          @Override
 *          public Object onCompletedExposed(Response response) throws Exception {
 *            ...
 *            if (result.isError())
 *              onError.call(new ErrorResult(result.getTextValue()));
 *            else
 *              onSuccess.call(result.getTextValue());
 *            return null;
 *          }
 *        }
 *      );
 *  }
 * </pre>
 *
 * 3. If Async is run as a scheduled task, it is still probably
 *    easier to provide fake request and enter fake RequestScope
 *    or make internal http call.
 */



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

  public final <F> Async<F> then(final Function<T, Async<F>> fn) {
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

  public final T awaitChecked() throws Throwable {
    AtomicReference<T> ret = new AtomicReference<T>();
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> ex = new AtomicReference<Throwable>();
    run(Callbacks.storeAndCountDown(ret, latch),
            Callbacks.storeAndCountDown(ex, latch));
    latch.await();
    if (ex.get() != null)
      throw ex.get();
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
