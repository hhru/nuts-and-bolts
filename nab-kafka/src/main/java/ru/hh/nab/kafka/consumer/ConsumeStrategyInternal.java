package ru.hh.nab.kafka.consumer;

import java.util.List;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;


class ConsumeStrategyInternal<T> {

  private volatile KafkaConsumer<T> kafkaConsumer;

  public void setKafkaConsumer(KafkaConsumer<T> kafkaConsumer) {
    this.kafkaConsumer = kafkaConsumer;
  }

  void invokeOnConsumer(List<ConsumerRecord<String, T>> messages, Consumer<?, ?> consumer) {
    kafkaConsumer.onMessagesBatch(messages, consumer);
  }

}
