package ru.hh.nab.performance.variants;

import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class PartiallyOverflowingCacheWithSizeAtomicCache<K, V> implements GenericCache<K, V> {
  private final ConcurrentHashMap<K, V> strongStorage = new ConcurrentHashMap<>();
  private final ConcurrentReferenceHashMap<K, V> weakStorage = new ConcurrentReferenceHashMap<>(16, 0.75f, 1,
      ConcurrentReferenceHashMap.ReferenceType.SOFT);
  private final int strongStorageMaxSize;
  private final AtomicBoolean strongStorageOverloaded = new AtomicBoolean(false);

  public PartiallyOverflowingCacheWithSizeAtomicCache(int strongStorageMaxSize) {
    this.strongStorageMaxSize = strongStorageMaxSize;
  }

  public int getStorageSize() {
    return strongStorage.size() + weakStorage.size();
  }

  public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
    if (!strongStorageOverloaded.get() && strongStorage.mappingCount() < strongStorageMaxSize) {
      return strongStorage.computeIfAbsent(key, mappingFunction);
    }

    strongStorageOverloaded.compareAndSet(false, true);

    V value = strongStorage.get(key);

    if (value != null) {
      return value;
    }

    return weakStorage.computeIfAbsent(key, mappingFunction);
  }
}
