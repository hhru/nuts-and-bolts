package ru.hh.nab.testbase.kafka;

import java.time.Duration;
import org.slf4j.Logger;
import ru.hh.nab.kafka.consumer.ConsumeStrategy;
import ru.hh.nab.kafka.consumer.ConsumerBuilder;
import ru.hh.nab.kafka.consumer.KafkaConsumer;
import ru.hh.nab.kafka.consumer.SeekPosition;
import ru.hh.nab.kafka.consumer.retry.RetryPolicyResolver;
import ru.hh.nab.kafka.consumer.retry.RetryTopics;
import ru.hh.nab.kafka.producer.KafkaProducer;

public class NoopConsumerBuilder<T> implements ConsumerBuilder<T> {

  @Override
  public ConsumerBuilder<T> withClientId(String clientId) {
    return this;
  }

  @Override
  public ConsumerBuilder<T> withOperationName(String operationName) {
    return this;
  }

  @Override
  public ConsumerBuilder<T> withConsumeStrategy(ConsumeStrategy<T> consumeStrategy) {
    return this;
  }

  @Override
  public ConsumerBuilder<T> withRetries(KafkaProducer retryProducer, RetryPolicyResolver<T> retryPolicyResolver, RetryTopics retryTopics) {
    return this;
  }

  @Override
  public ConsumerBuilder<T> withRetryConsumeStrategy(ConsumeStrategy<T> retryConsumeStrategy) {
    return this;
  }

  @Override
  public ConsumerBuilder<T> withLogger(Logger logger) {
    return this;
  }

  @Override
  public ConsumerBuilder<T> withConsumerGroup() {
    return this;
  }

  @Override
  public ConsumerBuilder<T> withAllPartitionsAssigned(SeekPosition seekPosition, Duration checkNewPartitionsInterval) {
    return this;
  }

  @Override
  public ConsumerBuilder<T> withAllPartitionsAssigned(SeekPosition seekPosition) {
    return this;
  }

  @Override
  public KafkaConsumer<T> start() {
    return new NoopKafkaConsumer<>();
  }
}
