package ru.hh.nab.starter.exceptions;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public final class ExceptionUtils {
  private ExceptionUtils() {
  }

  public static <T> T getOrThrowMappable(Callable<T> supplier) {
    try {
      return supplier.call();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new NabMappableException(e);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new NabMappableException(e);
    }
  }

  public static <T> T getOrThrowMappable(Future<T> future) {
    return getOrThrowMappable(future::get);
  }
}
