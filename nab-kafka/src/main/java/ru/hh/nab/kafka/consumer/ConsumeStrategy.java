package ru.hh.nab.kafka.consumer;

import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@FunctionalInterface
public interface ConsumeStrategy<T> {

  /**
   * Creates a ConsumeStrategy that processes messages at least once with batch acknowledgment and retries.
   * Messages are processed sequentially and if processing fails for a message, it will be retried via the Ack.retry() mechanism.
   * The batch is acknowledged only after all messages are successfully processed or explicitly retried.
   *
   * @param messageProcessor The processor that handles individual messages
   * @param <M>              The type of messages being processed
   * @return A ConsumeStrategy that processes messages at least once with batch acknowledgment and retries
   */
  static <M> ConsumeStrategy<M> atLeastOnceWithBatchAck(MessageProcessor<M> messageProcessor) {
    return (consumerRecords, ack) -> {
      for (ConsumerRecord<String, M> record : consumerRecords) {
        try {
          messageProcessor.process(record.value());
        } catch (RuntimeException e) {
          ack.retry(record, e);
        }
      }
      ack.acknowledge();
    };
  }

  /**
   * Creates a ConsumeStrategy that processes messages at least once with batch acknowledgment but without retries.
   * Each message is processed and explicitly acknowledged via seek() before moving to the next one.
   * If processing fails, the exception is propagated without retry attempts.
   *
   * @param messageProcessor The processor that handles individual messages
   * @param <M>              The type of messages being processed
   * @return A ConsumeStrategy that processes messages at least once with batch acknowledgment
   */
  static <M> ConsumeStrategy<M> atLeastOnceWithBatchAckWithoutRetries(MessageProcessor<M> messageProcessor) {
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
