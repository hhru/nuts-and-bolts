package ru.hh.nab.kafka.consumer.retry;

import java.util.function.BiPredicate;
import java.util.function.Predicate;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import ru.hh.nab.kafka.consumer.retry.policy.RetryPolicy;
import ru.hh.nab.kafka.util.PredicateChainResolver;

public class RetryPolicyResolverBuilder<T> extends PredicateChainResolver<ConsumerRecord<String, T>, Throwable, RetryPolicy> {

  public RetryPolicyResolverBuilder<T> registerRetryPolicy(BiPredicate<T, Throwable> predicate, RetryPolicy retryPolicy) {
    when((record, error) -> predicate.test(record.value(), error), retryPolicy);
    return this;
  }

  public RetryPolicyResolverBuilder<T> registerRetryPolicy(Predicate<Throwable> exceptionPredicate, RetryPolicy retryPolicy) {
    whenB(exceptionPredicate, retryPolicy);
    return this;
  }

  public RetryPolicyResolverBuilder<T> registerRetryPolicy(Class<? extends Throwable> exceptionClass, RetryPolicy retryPolicy) {
    return registerRetryPolicy(exceptionClass::isInstance, retryPolicy);
  }

  public RetryPolicyResolver<T> build() {
    return this::apply;
  }
}
