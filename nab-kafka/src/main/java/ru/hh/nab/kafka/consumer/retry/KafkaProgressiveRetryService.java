package ru.hh.nab.kafka.consumer.retry;

import java.util.Objects;
import java.util.function.BiFunction;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import ru.hh.nab.kafka.consumer.retry.policy.RetryPolicy;
import ru.hh.nab.kafka.producer.KafkaProducer;

public final class KafkaProgressiveRetryService<T> extends KafkaRetryService<T> {
  private final BiFunction<ConsumerRecord<String, T>, Throwable, RetryPolicy> retryPolicyResolver;

  public KafkaProgressiveRetryService(
      KafkaProducer retryProducer,
      String retryTopic,
      BiFunction<ConsumerRecord<String, T>, Throwable, RetryPolicy> retryPolicyResolver) {
    super(retryProducer, retryTopic);
    this.retryPolicyResolver = Objects.requireNonNull(retryPolicyResolver);
  }

  public KafkaProgressiveRetryService(
      KafkaProducer retryProducer,
      String retryTopic,
      MessageMetadataProvider messageMetadataProvider,
      BiFunction<ConsumerRecord<String, T>, Throwable, RetryPolicy> retryPolicyResolver
  ) {
    super(retryProducer, retryTopic, messageMetadataProvider);
    this.retryPolicyResolver = retryPolicyResolver;
  }

  @Override
  protected RetryPolicy getRetryPolicy(ConsumerRecord<String, T> message, Throwable error) {
    return retryPolicyResolver.apply(message, error);
  }
}
