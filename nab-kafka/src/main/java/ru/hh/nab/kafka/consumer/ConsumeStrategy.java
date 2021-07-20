package ru.hh.nab.kafka.consumer;

import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@FunctionalInterface
public interface ConsumeStrategy<T> {

  void onMessagesBatch(List<ConsumerRecord<String, T>> messages, Ack<T> ack) throws InterruptedException;

}
