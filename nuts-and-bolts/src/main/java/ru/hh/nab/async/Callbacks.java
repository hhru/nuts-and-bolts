package ru.hh.nab.async;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;

public abstract class Callbacks {
  public static class EmptyCallback<T> implements Callback<T> {
    @Override
    public void call(T arg) throws Exception {
    }
  }

  public static final Callback<?> EMPTY = new EmptyCallback<Object>();

  @SuppressWarnings({"unchecked"})
  public static <T> Callback<T> empty() {
    return (Callback<T>) EMPTY;
  }

  public static class StoreIntegerCallback implements Callback<Integer> {
    private final AtomicInteger ref;

    public StoreIntegerCallback(AtomicInteger ref) {
      this.ref = ref;
    }

    @Override
    public void call(Integer arg) throws Exception {
      ref.set(arg);
    }
  }

  public StoreIntegerCallback storeInt(AtomicInteger ref) {
    return new StoreIntegerCallback(ref);
  }

  public static class StoreCallback<T> implements Callback<T> {
    private final AtomicReference<T> ref;

    public StoreCallback(AtomicReference<T> ref) {
      this.ref = ref;
    }

    @Override
    public void call(T arg) throws Exception {
      ref.set(arg);
    }
  }

  public static <T> StoreCallback<T> store(AtomicReference<T> obj) {
    return new StoreCallback<T>(obj);
  }

  public static enum LogLevel {
    ERROR {
      @Override
      public void log(Logger l, String msg, Object o) {
        l.error(msg, o);
      }
    }, WARN {
      @Override
      public void log(Logger l, String msg, Object o) {
        l.warn(msg, o);
      }
    }, INFO {
      @Override
      public void log(Logger l, String msg, Object o) {
        l.info(msg, o);
      }
    }, DEBUG {
      @Override
      public void log(Logger l, String msg, Object o) {
        l.debug(msg, o);
      }
    }, TRACE {
      @Override
      public void log(Logger l, String msg, Object o) {
        l.trace(msg, o);
      }
    };

    public abstract void log(Logger l, String msg, Object o);
  }

  public static class LogCallback<T> implements Callback<T> {
    private final Logger log;
    private final String msg;
    private final LogLevel lv;

    public LogCallback(Logger log, String msg, LogLevel lv) {
      this.log = log;
      this.msg = msg;
      this.lv = lv;
    }

    @Override
    public void call(T arg) throws Exception {
      lv.log(log, msg, arg);
    }
  }

  public static LogCallback<Throwable> logError(Logger log, String msg) {
    return new LogCallback<Throwable>(log, msg, LogLevel.ERROR);
  }

  public static LogCallback<Throwable> logWarn(Logger log, String msg) {
    return new LogCallback<Throwable>(log, msg, LogLevel.WARN);
  }

  public static LogCallback<Throwable> logInfo(Logger log, String msg) {
    return new LogCallback<Throwable>(log, msg, LogLevel.INFO);
  }

  public static LogCallback<Throwable> logDebug(Logger log, String msg) {
    return new LogCallback<Throwable>(log, msg, LogLevel.DEBUG);
  }

  public static LogCallback<Throwable> logTrace(Logger log, String msg) {
    return new LogCallback<Throwable>(log, msg, LogLevel.TRACE);
  }

  public static class CountDownCallback<T> implements Callback<T> {
    private final CountDownLatch latch;

    public CountDownCallback(CountDownLatch latch) {
      this.latch = latch;
    }

    @Override
    public void call(T arg) throws Exception {
      latch.countDown();
    }
  }

  public static <T> CountDownCallback<T> countDown(CountDownLatch latch) {
    return new CountDownCallback<T>(latch);
  }

  public static class StoreAndCountDownCallback<T> implements Callback<T> {
    private final CountDownLatch latch;
    private final AtomicReference<T> ref;

    public StoreAndCountDownCallback(AtomicReference<T> ref, CountDownLatch latch) {
      this.ref = ref;
      this.latch = latch;
    }

    @Override
    public void call(T v) throws Exception {
      ref.set(v);
      latch.countDown();
    }
  }

  public static <T> StoreAndCountDownCallback<T> storeAndCountDown(AtomicReference<T> ref, CountDownLatch latch) {
    return new StoreAndCountDownCallback<T>(ref, latch);
  }
}
