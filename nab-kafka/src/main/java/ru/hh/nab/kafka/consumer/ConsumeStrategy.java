package ru.hh.nab.kafka.consumer;

import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.errors.SerializationException;

@FunctionalInterface
public interface ConsumeStrategy<T> {

  static <M> ConsumeStrategy<M> atLeastOnceWithBatchAck(MessageProcessor<M> messageProcessor) {
    return (consumerRecords, ack) -> {
      for (ConsumerRecord<String, M> record : consumerRecords) {
        try {
          messageProcessor.process(record.value());
          ack.seek(record);
        } catch (SerializationException e) {
          ack.nAcknowledge(record);
        } catch (RuntimeException e) {
          ack.retry(record, e);
        }
      }
      ack.acknowledge();
    };
  }

  void onMessagesBatch(List<ConsumerRecord<String, T>> messages, Ack<T> ack) throws InterruptedException;

}
