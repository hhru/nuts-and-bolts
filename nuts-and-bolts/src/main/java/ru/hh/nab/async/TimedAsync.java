package ru.hh.nab.async;

import com.google.common.base.Function;
import ru.hh.nab.health.monitoring.TimingsLogger;
import ru.hh.nab.scopes.RequestScope;

public abstract class TimedAsync<T> extends Async<T> {

  protected abstract TimingsLogger getTimingsLogger();

  public final <F> TimedAsync<F> thenTimed(final String probe, final Function<T, Async<F>> fn) {
    final TimingsLogger timingsLogger = getTimingsLogger();

    return new TimedAsync<F>() {
      @Override
      public void runExposed(final Callback<F> onSuccess, final Callback<Throwable> onError) {
        TimedAsync.this.run(
            new Callback<T>() {
              @Override
              public void call(T result) {
                try {
                  timingsLogger.enterTimedArea();
                  getTimingsLogger().probe(probe);
                  fn.apply(result).run(onSuccess, onError);
                } finally {
                  timingsLogger.leaveTimedArea();
                }
              }
            },
            onError
        );
      }

      @Override
      protected TimingsLogger getTimingsLogger() {
        return timingsLogger;
      }
    };
  }

  public static TimedAsync<Void> startTimedAsync() {
    return startTimedAsync(RequestScope.currentTimingsLogger());
  }

  public static TimedAsync<Void> startTimedAsync(final TimingsLogger timingsLogger) {
    return new TimedAsync<Void>() {
      @Override
      protected void runExposed(Callback<Void> onSuccess, Callback<Throwable> onError) throws Exception {
        onSuccess.call(null);
      }

      @Override
      protected TimingsLogger getTimingsLogger() {
        return timingsLogger;
      }
    };
  }
}
