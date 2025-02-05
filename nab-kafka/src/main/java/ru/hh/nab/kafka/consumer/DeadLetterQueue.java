package ru.hh.nab.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import ru.hh.nab.kafka.producer.KafkaProducer;

class DeadLetterQueue<V> {
  private final String topicName;
  private final KafkaProducer producer;

  public DeadLetterQueue(String topicName, KafkaProducer producer) {
    this.topicName = topicName;
    this.producer = producer;
  }

  public void send(ConsumerRecord<String, V> record) {
    this.send(toProducerRecord(record));
  }

  public void send(ProducerRecord<String, V> record) {
    this.producer.sendMessage(record, Runnable::run);
  }

  private ProducerRecord<String, V> toProducerRecord(ConsumerRecord<String, V> consumerRecord) {
    return new ProducerRecord<>(topicName, consumerRecord.key(), consumerRecord.value());
  }
}
