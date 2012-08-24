package ru.hh.nab.scopes;

import ru.hh.nab.async.Callback;
import ru.hh.nab.scopes.RequestScope.RequestScopeClosure;

// for use with tasks which call callbacks in other threads
public abstract class CallbackWithRequestScope<T> implements Callback<T> {
  private final RequestScopeClosure cls;
  public CallbackWithRequestScope() {
    cls = RequestScope.currentClosure();
  }

  @Override
  public final void call(T value) throws Exception {
    try {
      cls.enter();
      callExposed(value);
    } finally {
      cls.leave();
    }
  }

  protected abstract void callExposed(T value) throws Exception;

  public static <T> Callback<T> fromCallback(final Callback<T> callback) {
    return new CallbackWithRequestScope<T>() {
      @Override
      protected void callExposed(T value) throws Exception {
        callback.call(value);
      }
    };
  }
}
