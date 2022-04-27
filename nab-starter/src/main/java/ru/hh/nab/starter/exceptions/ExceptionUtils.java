package ru.hh.nab.starter.exceptions;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public final class ExceptionUtils {
  private ExceptionUtils() {
  }

  public static <T> T getOrThrowMappable(Callable<T> supplier) {
    return ru.hh.nab.common.util.ExceptionUtils.getOrThrow(supplier, NabMappableException::new);
  }

  public static <T> T getOrThrowMappable(Future<T> future) {
    return getOrThrowMappable(future::get);
  }
}
