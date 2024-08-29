package ru.hh.nab.kafka.consumer;

import java.util.function.BiPredicate;
import java.util.function.Predicate;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import ru.hh.nab.kafka.consumer.retry.policy.RetryPolicy;
import ru.hh.nab.kafka.util.PredicateChainResolver;

public class RetryPolicyResolverBuilder<T> {
  private final PredicateChainResolver<ConsumerRecord<String, T>, Throwable, RetryPolicy> retryPolicyResolver = new PredicateChainResolver<>();

  public RetryPolicyResolverBuilder<T> registerRetryPolicy(BiPredicate<T, Throwable> predicate, RetryPolicy retryPolicy) {
    retryPolicyResolver.when((record, error) -> predicate.test(record.value(), error), retryPolicy);
    return this;
  }

  public RetryPolicyResolverBuilder<T> registerRetryPolicy(Predicate<Throwable> exceptionPredicate, RetryPolicy retryPolicy) {
    retryPolicyResolver.whenB(exceptionPredicate, retryPolicy);
    return this;
  }

  public RetryPolicyResolverBuilder<T> registerRetryPolicy(Class<? extends Throwable> exceptionClass, RetryPolicy retryPolicy) {
    return registerRetryPolicy(exceptionClass::isInstance, retryPolicy);
  }

  public PredicateChainResolver<ConsumerRecord<String, T>, Throwable, RetryPolicy> build() {
    return retryPolicyResolver;
  }
}
