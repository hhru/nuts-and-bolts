package ru.hh.nab.kafka.consumer.retry;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import ru.hh.nab.kafka.consumer.retry.policy.RetryPolicy;
import ru.hh.nab.kafka.producer.KafkaProducer;

public final class KafkaFixedDelayRetryService<T> extends KafkaRetryService<T> {
  private final RetryPolicy retryPolicy;

  public KafkaFixedDelayRetryService(
      KafkaProducer retryProducer,
      String retryTopic,
      RetryPolicy retryPolicy) {
    super(retryProducer, retryTopic);
    if (!retryPolicy.hasFixedDelay()) {
      throw new IllegalArgumentException("Retry policy must have fixed delay");
    }
    this.retryPolicy = retryPolicy;
  }

  public KafkaFixedDelayRetryService(
      KafkaProducer retryProducer,
      String retryTopic,
      MessageMetadataProvider messageMetadataProvider,
      RetryPolicy retryPolicy
  ) {
    super(retryProducer, retryTopic, messageMetadataProvider);
    this.retryPolicy = retryPolicy;
  }

  @Override
  protected RetryPolicy getRetryPolicy(ConsumerRecord<String, T> message, Throwable error) {
    return retryPolicy;
  }
}
