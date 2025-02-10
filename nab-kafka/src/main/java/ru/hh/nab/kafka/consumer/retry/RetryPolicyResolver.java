package ru.hh.nab.kafka.consumer.retry;

import java.util.function.BiFunction;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import ru.hh.nab.kafka.consumer.retry.policy.RetryPolicy;

/**
 * Decides what {@link RetryPolicy} to use for concrete message and processing error
 */
@FunctionalInterface
public interface RetryPolicyResolver<T> extends BiFunction<ConsumerRecord<String, T>, Throwable, RetryPolicy> {

  static <T> RetryPolicyResolver<T> never() {
    return (consumerRecord, throwable) -> RetryPolicy.never();
  }

  static <T> RetryPolicyResolver<T> always(RetryPolicy retryPolicy) {
    return (consumerRecord, throwable) -> retryPolicy;
  }
}
