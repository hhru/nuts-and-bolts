package ru.hh.nab.kafka.consumer;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

public interface Ack<T> {
  /**
   * Move committedOffset and fetchOffset to the offsets returned on the last Consumer.poll() for the subscribed list of topics and partitions
   *
   * fetchOffset is used on the next poll request to Kafka broker.
   * committedOffset is used in case if the node goes down, or in case if re-balancing happened.
   *
   * This method must be executed only within the consumer listener thread.
   */
  void acknowledge();

  /**
   * Move both Kafka committedOffset and fetchOffset
   * belonging to the topic and partition of the passed record
   *
   * fetchOffset is used on the next poll request to Kafka broker.
   * committedOffset is used in case if the node goes down, or in case if re-balancing happened.
   *
   * This method must be executed only within the consumer listener thread.
   *
   * @param message - object containing information about topic, partition and offset to use to move offsets
   */
  default void acknowledge(ConsumerRecord<String, T> message) {
    commit(List.of(message));
    seek(message);
  }

  /**
   * Move both Kafka committedOffset and fetchOffset
   * belonging to the last record per each topic and partition from the passed messages
   *
   * fetchOffset is used on the next poll request to Kafka broker.
   * committedOffset is used in case if the node goes down, or in case if re-balancing happened.
   *
   * This method must be executed only within the consumer listener thread.
   *
   * @param messages - collection of objects that containing information about topic, partition and offset to use to move offsets
   */
  default void acknowledge(Collection<ConsumerRecord<String, T>> messages) {
    commit(messages);
    seek(messages);
  }

  /**
   * Move fetchOffset of the message topic and partition to the position next after message offset.
   *
   * fetchOffset is used on the next poll request to Kafka broker.
   *
   * This method must be executed only within the consumer listener thread.
   *
   * @param message - object containing information about topic, partition and offset to use to move internal fetchOffset
   */
  default void seek(ConsumerRecord<String, T> message) {
    seek(List.of(message));
  }

  /**
   * Move fetchOffset of the message topic and partition to the position after the last message offset.
   *
   * fetchOffset is used on the next poll request to Kafka broker.
   *
   * This method must be executed only within the consumer listener thread.
   *
   * @param message - collection of objects containing information about topic, partition and offset to use to move internal fetchOffset
   */
  void seek(Collection<ConsumerRecord<String, T>> messages);

  /**
   * Commit the specified offsets belonging to the passed messages collection for the specified list of topics and partitions to Kafka.
   *
   * This method is safe to use from any thread,
   * however the thread must guarantee the ascending order of the messages passed to this method
   *
   * This method is intended to be used only in case of async processing of the messages,
   * i.e. in case when  consumed messages are handled to some processor thread via blocking queue.
   * Think twice before using it!
   *
   * @param messages - collection of objects containing information about topic, partition and offset to use to move committedOffset
   */
  void commit(Collection<ConsumerRecord<String, T>> messages);
}
