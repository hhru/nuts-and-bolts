package ru.hh.nab.common.util;

import java.util.concurrent.Callable;
import java.util.function.Function;

public final class ExceptionUtils {
  private ExceptionUtils() {
  }

  public static <T> T getOrThrow(Callable<T> callable) {
    return getOrThrow(callable, RuntimeException::new);
  }

  public static <T, E extends RuntimeException> T getOrThrow(Callable<T> callable,
                                                             Function<Throwable, E> checkedExceptionMapper) {
    try {
      return callable.call();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw checkedExceptionMapper.apply(e);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw checkedExceptionMapper.apply(e);
    }
  }
}
