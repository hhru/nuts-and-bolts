package ru.hh.nab.kafka.consumer;

import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import ru.hh.nab.kafka.producer.KafkaProducer;
import ru.hh.nab.kafka.producer.KafkaSendResult;

class DeadLetterQueue<V> {
  private final String topicName;
  private final KafkaProducer producer;

  public DeadLetterQueue(String topicName, KafkaProducer producer) {
    this.topicName = topicName;
    this.producer = producer;
  }

  public CompletableFuture<KafkaSendResult<V>> send(ConsumerRecord<String, V> record) {
    return this.send(toProducerRecord(record));
  }

  public CompletableFuture<KafkaSendResult<V>> send(ProducerRecord<String, V> record) {
    return this.producer.sendMessage(record, Runnable::run);
  }

  private ProducerRecord<String, V> toProducerRecord(ConsumerRecord<String, V> consumerRecord) {
    return new ProducerRecord<>(topicName, null, consumerRecord.key(), consumerRecord.value(), consumerRecord.headers());
  }
}
