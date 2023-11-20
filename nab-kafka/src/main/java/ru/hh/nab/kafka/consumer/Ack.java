package ru.hh.nab.kafka.consumer;

import java.util.Collection;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface Ack<T> {

  void acknowledge();

  void acknowledge(ConsumerRecord<String, T> message);

  void acknowledge(Collection<ConsumerRecord<String, T>> messages);

  void seek(ConsumerRecord<String, T> message);

}
