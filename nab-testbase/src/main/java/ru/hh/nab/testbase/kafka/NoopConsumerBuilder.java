package ru.hh.nab.testbase.kafka;

import java.time.Duration;
import org.slf4j.Logger;
import ru.hh.nab.kafka.consumer.AckProvider;
import ru.hh.nab.kafka.consumer.ConsumeStrategy;
import ru.hh.nab.kafka.consumer.ConsumerBuilder;
import ru.hh.nab.kafka.consumer.KafkaConsumer;
import ru.hh.nab.kafka.consumer.SeekPosition;
import ru.hh.nab.kafka.consumer.retry.RetryService;

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
  public ConsumerBuilder<T> withRetryService(RetryService<T> retryService) {
    return this;
  }

  @Override
  public ConsumerBuilder<T> withLogger(Logger logger) {
    return this;
  }

  @Override
  public ConsumerBuilder<T> withAckProvider(AckProvider<T> ackProvider) {
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
