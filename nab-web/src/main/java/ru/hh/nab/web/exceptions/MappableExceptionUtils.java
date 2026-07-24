package ru.hh.nab.web.exceptions;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import ru.hh.platform.utils.util.ExceptionUtils;

public final class MappableExceptionUtils {
  private MappableExceptionUtils() {
  }

  public static <T> T getOrThrowMappable(Callable<T> supplier) {
    return ExceptionUtils.getOrThrow(supplier, NabMappableException::new);
  }

  public static <T> T getOrThrowMappable(Future<T> future) {
    return getOrThrowMappable(future::get);
  }
}
