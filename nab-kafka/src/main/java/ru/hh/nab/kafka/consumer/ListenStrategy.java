package ru.hh.nab.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import java.util.List;

public interface ListenStrategy<T> {

  void onMessagesBatch(List<ConsumerRecord<String, T>> messages, Ack ack);

}
