package ru.hh.nab.web.jersey.resolver;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.springframework.util.ConcurrentReferenceHashMap;

class PartiallyOverflowingCache<K, V> {
  private final ConcurrentHashMap<K, V> strongStorage = new ConcurrentHashMap<>();
  private final ConcurrentReferenceHashMap<K, V> weakStorage = new ConcurrentReferenceHashMap<>(16, 0.75f, 1,
      ConcurrentReferenceHashMap.ReferenceType.SOFT);
  private final int strongStorageMaxSize;

  PartiallyOverflowingCache(int strongStorageMaxSize) {
    this.strongStorageMaxSize = strongStorageMaxSize;
  }

  int getStorageSize() {
    return strongStorage.size() + weakStorage.size();
  }

  /**
   * Может позволить себе допустить гонки потому что:
   * а) количество лишних записей не будет превышать количество потоков, одновременно записывающих разные ключи
   * б) городить на этом этапе дополнительную синхронизацию кажется избыточным, т.к. переполнение это "аварийный" режим работы кэша.
   * jmh test {@link ru.hh.nab.web.jersey.resolver.PartiallyOverflowingCachePerformanceTest}
   */
  V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
    if (strongStorage.mappingCount() < strongStorageMaxSize) {
      return strongStorage.computeIfAbsent(key, mappingFunction);
    }

    V value = strongStorage.get(key);

    if (value != null) {
      return value;
    }

    return weakStorage.computeIfAbsent(key, mappingFunction);
  }
}
