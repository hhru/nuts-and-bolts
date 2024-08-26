package ru.hh.nab.kafka.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Similar to switch, chooses result based on predicate chain. Add predicates and corresponding result values to chain
 * using {@link #when(BiPredicate, Object)}, {@link #whenA(Predicate, Object)} and {@link #whenB(Predicate, Object)} methods.
 * <p>
 * PredicateChainResolver applies predicates to inputs <b>a</b> and <b>b</b> in order they were added,
 * and returns the result associated with the first predicate that matches them. If no predicate matches then {@code null} is returned
 */
public class PredicateChainResolver<A, B, R> implements BiFunction<A, B, R> {
  final LinkedHashMap<BiPredicate<A, B>, R> predicateToResult = new LinkedHashMap<>();
  R defaultResult;

  public PredicateChainResolver(R defaultResult) {
    this.defaultResult = defaultResult;
  }

  @Override
  public R apply(A a, B b) {
    return predicateToResult
        .entrySet()
        .stream()
        .filter(entry -> entry.getKey().test(a, b))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(defaultResult);
  }

  public PredicateChainResolver<A, B, R> when(BiPredicate<A, B> predicate, R result) {
    predicateToResult.put(predicate, result);
    return this;
  }

  public PredicateChainResolver<A, B, R> whenA(Predicate<A> predicate, R result) {
    return when(((a, b) -> predicate.test(a)), result);
  }

  public PredicateChainResolver<A, B, R> whenB(Predicate<B> predicate, R result) {
    return when(((a, b) -> predicate.test(b)), result);
  }
}
