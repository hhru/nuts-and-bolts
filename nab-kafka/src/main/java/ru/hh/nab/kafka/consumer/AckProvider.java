package ru.hh.nab.kafka.consumer;

import org.apache.kafka.clients.consumer.Consumer;
import ru.hh.nab.kafka.consumer.retry.RetryService;

@FunctionalInterface
public interface AckProvider<T> {
  Ack<T> createAck(KafkaConsumer<T> kafkaConsumer, Consumer<?, ?> nativeKafkaConsumer, RetryService<T> retryService);
}
