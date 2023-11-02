package ru.hh.nab.kafka.consumer;

import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@FunctionalInterface
public interface ConsumeStrategy<T> {

  static <M> ConsumeStrategy<M> atLeastOnceWithBatchAck(MessageProcessor<M> messageProcessor) {
    return (consumerRecords, ack) -> {
      for (ConsumerRecord<String, M> record : consumerRecords) {
        messageProcessor.process(record.value());
        ack.seek(record);
      }
      ack.acknowledge();
    };
  }

  void onMessagesBatch(List<ConsumerRecord<String, T>> messages, Ack<T> ack) throws InterruptedException;

}
