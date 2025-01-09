package ru.hh.nab.web.jersey.resolver.variants;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.springframework.util.ConcurrentReferenceHashMap;

public class PartiallyOverflowingCacheWithSizeCache<K, V> implements GenericCache<K, V> {
  private final ConcurrentHashMap<K, V> strongStorage = new ConcurrentHashMap<>();
  private final ConcurrentReferenceHashMap<K, V> weakStorage = new ConcurrentReferenceHashMap<>(16, 0.75f, 1,
      ConcurrentReferenceHashMap.ReferenceType.SOFT);
  private final int strongStorageMaxSize;
  private boolean strongStorageOverloaded = false;

  public PartiallyOverflowingCacheWithSizeCache(int strongStorageMaxSize) {
    this.strongStorageMaxSize = strongStorageMaxSize;
  }

  public int getStorageSize() {
    return strongStorage.size() + weakStorage.size();
  }

  public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
    if (!strongStorageOverloaded && strongStorage.mappingCount() < strongStorageMaxSize) {
      return strongStorage.computeIfAbsent(key, mappingFunction);
    }

    if (!strongStorageOverloaded) {
      strongStorageOverloaded = true;
    }

    V value = strongStorage.get(key);

    if (value != null) {
      return value;
    }

    return weakStorage.computeIfAbsent(key, mappingFunction);
  }
}
