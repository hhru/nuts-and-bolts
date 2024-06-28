package ru.hh.nab.kafka.consumer.retry;

import java.util.function.BiFunction;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import ru.hh.nab.kafka.consumer.retry.policy.RetryPolicy;

@FunctionalInterface
public interface RetryPolicyResolver<T> extends BiFunction<ConsumerRecord<String, T>, Throwable, RetryPolicy> {

  static <T> RetryPolicyResolver<T> never() {
    return (consumerRecord, throwable) -> RetryPolicy.never();
  }

  static <T> RetryPolicyResolver<T> always(RetryPolicy retryPolicy) {
    return (consumerRecord, throwable) -> retryPolicy;
  }
}
