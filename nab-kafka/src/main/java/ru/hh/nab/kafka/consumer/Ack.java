package ru.hh.nab.kafka.consumer;

import java.util.Collection;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface Ack<T> {
  /**
   * Under the hood is moving both Kafka committedOffset and fetchOffset to the same position,
   * equal to the last record from each topic and partition of the previously polled batch.
   *
   * Fetch offset is used on the next poll request to Kafka broker.
   * Committed offset is used in case if the node goes down, or in case if re-balancing happened.
   *
   * This method must be executed only within the consumer listener thread.
   */
  void acknowledge();

  /**
   * Under the hood is moving both Kafka committedOffset and fetchOffset,
   * belonging the topic and partition of the message,
   * to the same position equal to the last record of the previously polled batch.
   *
   * Fetch offset is used on the next poll request to Kafka broker.
   * Committed offset is used in case if the node goes down, or in case if re-balancing happened.
   *
   * This method must be executed only within the consumer listener thread.
   *
   * @param message - object containing information about topic, partition and offset to use to move offsets
   */
  void acknowledge(ConsumerRecord<String, T> message);

  /**
   * Under the hood is moving both Kafka committedOffset and fetchOffset to the same position,
   * equal to the last record from each topic and partition of the messages from passed messages collection
   *
   * Fetch offset is used on the next poll request to Kafka broker.
   * Committed offset is used in case if the node goes down, or in case if re-balancing happened.
   *
   * This method must be executed only within the consumer listener thread.
   *
   * @param messages - collection of objects that containing information about topic, partition and offset to use to move offsets
   */
  void acknowledge(Collection<ConsumerRecord<String, T>> messages);

  /**
   * Under the hood is moving fetchOffset of the message topic and partition to the message offset.
   *
   * Fetch offset is used on the next poll request to Kafka broker.
   *
   * This method must be executed only within the consumer listener thread.
   *
   * @param message - object containing information about topic, partition and offset to use to move internal fetch offset
   */
  void seek(ConsumerRecord<String, T> message);

  /**
   * Under the hood commit the specified offsets belonging to the passed messages collection for the specified list of topics and partitions to Kafka.
   *
   * This method is safe to use from any thread,
   * however the thread must guarantee the ascending order of the messages passed to this method
   *
   * This method is intended to be used only in case of async processing of the messages,
   * i.e. in case when  consumed messages are handled to some processor thread via blocking queue.
   * Think twice before using it!
   *
   * @param messages - collection of objects containing information about topic, partition and offset to use to move committed offset
   */
  void commitOnly(Collection<ConsumerRecord<String, T>> messages);
}
