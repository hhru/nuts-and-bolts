package ru.hh.nab.performance.variants;

import java.util.function.Function;

public interface GenericCache<K, V> {
  public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction);
}

