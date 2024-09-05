package ru.hh.nab.kafka.consumer;

import org.apache.kafka.clients.consumer.Consumer;

@FunctionalInterface
public interface AckProvider<T> {
  Ack<T> createAck(KafkaConsumer<T> kafkaConsumer, Consumer<?, ?> nativeKafkaConsumer, RetryService<T> retryService);
}
