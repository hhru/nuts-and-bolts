package ru.hh.nab.performance.variants;

import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class PartiallyOverflowingCacheOptional<K, V> implements GenericCache<K, V> {
  private final ConcurrentHashMap<K, V> strongStorage = new ConcurrentHashMap<>();
  private final ConcurrentReferenceHashMap<K, V> weakStorage = new ConcurrentReferenceHashMap<>(16, 0.75f, 1,
      ConcurrentReferenceHashMap.ReferenceType.SOFT);
  private final int strongStorageMaxSize;

  public PartiallyOverflowingCacheOptional(int strongStorageMaxSize) {
    this.strongStorageMaxSize = strongStorageMaxSize;
  }

  public int getStorageSize() {
    return strongStorage.size() + weakStorage.size();
  }

  public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
    if (strongStorage.mappingCount() < strongStorageMaxSize) {
      return strongStorage.computeIfAbsent(key, mappingFunction);
    }

    return Optional.ofNullable(strongStorage.get(key)).orElseGet(() -> weakStorage.computeIfAbsent(key, mappingFunction));
  }
}
