package ru.hh.util.trees;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.annotation.concurrent.Immutable;

/**
 * @param <K> key - by which subtrees are indexed
 * @param <P> payload - SHOULD be immutable or else this class wouldn't too
 */
@Immutable
public final class ExternalTree<K, P> implements Tree<K, P, ExternalTree<K, P>> {
  private final Map<K, ExternalTree<K, P>> ways;
  private final P payload;

  public ExternalTree(P payload, Map<K, ExternalTree<K, P>> ways) {
    this.payload = payload;
    this.ways = ways;
  }

  public ExternalTree(P payload) {
    this.payload = payload;
    this.ways = ImmutableMap.of();
  }

  public static <K, P> ExternalTree<K, P> of(P payload, Map<K, ExternalTree<K, P>> ways) {
    return new ExternalTree<K, P>(payload, ways);
  }

  public static <K, P> ExternalTree<K, P> of(P payload) {
    return new ExternalTree<K, P>(payload);
  }

  public static <K, P> ExternalTree<K, P> of(P payload, K k1, ExternalTree<K, P> ways1) {
    return new ExternalTree<K, P>(payload, ImmutableMap.of(k1, ways1));
  }

  public static <K, P> ExternalTree<K, P> of(P payload, K k1, ExternalTree<K, P> ways1, K k2,
                                             ExternalTree<K, P> ways2) {
    return new ExternalTree<K, P>(payload, ImmutableMap.of(k1, ways1, k2, ways2));
  }

  public static <K, P> ExternalTree<K, P> of(P payload, K k1, ExternalTree<K, P> ways1, K k2, ExternalTree<K, P> ways2,
                                             K k3, ExternalTree<K, P> ways3) {
    return new ExternalTree<K, P>(payload, ImmutableMap.of(k1, ways1, k2, ways2, k3, ways3));
  }

  @Override
  public P get() {
    return payload;
  }

  @Override
  public ExternalTree<K, P> sub(K way) {
    return ways.get(way);
  }

  @Override
  public Iterable<ExternalTree<K, P>> subs() {
    return ways.values();
  }
}
