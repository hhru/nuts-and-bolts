package ru.hh.nab.kafka.consumer.retry;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import ru.hh.nab.kafka.consumer.retry.policy.FixedDelay;
import ru.hh.nab.kafka.producer.KafkaProducer;

public final class KafkaFixedDelayRetryService<T> extends KafkaRetryService<T> {
  private final FixedDelay retryPolicy;

  public KafkaFixedDelayRetryService(
      KafkaProducer retryProducer,
      String retryTopic,
      FixedDelay retryPolicy) {
    super(retryProducer, retryTopic);
    this.retryPolicy = retryPolicy;
  }

  public KafkaFixedDelayRetryService(
      KafkaProducer retryProducer,
      String retryTopic,
      MessageMetadataProvider messageMetadataProvider,
      FixedDelay retryPolicy
  ) {
    super(retryProducer, retryTopic, messageMetadataProvider);
    this.retryPolicy = retryPolicy;
  }

  @Override
  protected RetryPolicy getRetryPolicy(ConsumerRecord<String, T> message, Throwable error) {
    return retryPolicy;
  }
}
