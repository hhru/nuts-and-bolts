package ru.hh.nab.kafka.consumer.retry;

import java.util.function.BiPredicate;
import java.util.function.Predicate;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import ru.hh.nab.kafka.consumer.retry.policy.RetryPolicy;
import ru.hh.nab.kafka.util.PredicateChainResolver;

public class RetryPolicyResolverBuilder<T> extends PredicateChainResolver<ConsumerRecord<String, T>, Throwable, RetryPolicy> {

  public RetryPolicyResolverBuilder(RetryPolicy defaultPolicy) {
    super(defaultPolicy);
  }

  public RetryPolicyResolverBuilder<T> registerRetryPolicy(BiPredicate<T, Throwable> predicate, RetryPolicy retryPolicy) {
    when((record, error) -> predicate.test(record.value(), error), retryPolicy);
    return this;
  }

  public RetryPolicyResolverBuilder<T> onException(Predicate<Throwable> exceptionPredicate, RetryPolicy retryPolicy) {
    whenB(exceptionPredicate, retryPolicy);
    return this;
  }

  public RetryPolicyResolverBuilder<T> onException(Class<? extends Throwable> exceptionClass, RetryPolicy retryPolicy) {
    return onException(exceptionClass::isInstance, retryPolicy);
  }

  public RetryPolicyResolverBuilder<T> neverRetry(Class<? extends Throwable> exceptionClass) {
    return onException(exceptionClass, RetryPolicy.never());
  }

  public RetryPolicyResolver<T> build() {
    return this::apply;
  }
}
