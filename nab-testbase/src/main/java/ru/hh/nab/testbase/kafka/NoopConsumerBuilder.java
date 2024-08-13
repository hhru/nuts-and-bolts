package ru.hh.nab.testbase.kafka;

import java.time.Duration;
import java.util.function.BiFunction;
import org.apache.kafka.clients.consumer.Consumer;
import org.slf4j.Logger;
import ru.hh.nab.kafka.consumer.Ack;
import ru.hh.nab.kafka.consumer.ConsumeStrategy;
import ru.hh.nab.kafka.consumer.ConsumerBuilder;
import ru.hh.nab.kafka.consumer.KafkaConsumer;
import ru.hh.nab.kafka.consumer.SeekPosition;

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
  public ConsumerBuilder<T> withLogger(Logger logger) {
    return this;
  }

  @Override
  public ConsumerBuilder<T> withAckProvider(BiFunction<KafkaConsumer<T>, Consumer<?, ?>, Ack<T>> ackProvider) {
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
