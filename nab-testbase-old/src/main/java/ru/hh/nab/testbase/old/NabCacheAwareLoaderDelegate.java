package ru.hh.nab.testbase.old;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate;

public class NabCacheAwareLoaderDelegate extends DefaultCacheAwareContextLoaderDelegate {

  private final ConcurrentMap<MergedContextConfiguration, RuntimeException> contextExceptionCache = new ConcurrentHashMap<>();

  @Override
  public ApplicationContext loadContext(MergedContextConfiguration mergedContextConfiguration) {
    RuntimeException exception = contextExceptionCache.get(mergedContextConfiguration);
    if (exception != null) {
      throw exception;
    }
    try {
      return super.loadContext(mergedContextConfiguration);
    } catch (Exception e) {
      contextExceptionCache.put(mergedContextConfiguration, e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e));
      throw e;
    }
  }
}
