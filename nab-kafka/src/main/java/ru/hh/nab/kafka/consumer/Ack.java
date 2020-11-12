package ru.hh.nab.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface Ack<T> {

  void acknowledge();

  void acknowledge(ConsumerRecord<String, T> lastProcessedRecord);

  void seek(ConsumerRecord<String, T> lastProcessedRecord);

  boolean isAcknowledge();

}
