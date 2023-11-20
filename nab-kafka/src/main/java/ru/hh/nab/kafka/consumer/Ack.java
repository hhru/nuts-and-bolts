package ru.hh.nab.kafka.consumer;

import java.util.Collection;
import org.apache.kafka.clients.consumer.ConsumerRecord;

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
   * This method must be executed only within the consumer listener thread otherwise ConcurrentModificationException will be thrown.
   *
   * @param message - object containing information about topic, partition and offset to use to move offsets
   */
  void acknowledge(ConsumerRecord<String, T> message);

  /**
   * Move both Kafka committedOffset and fetchOffset
   * belonging to the topic and partition of the passed record
   *
   * fetchOffset is used on the next poll request to Kafka broker.
   * committedOffset is used in case if the node goes down, or in case if re-balancing happened.
   *
   * This method must be executed only within the consumer listener thread otherwise ConcurrentModificationException will be thrown.
   *
   * @param message - object containing information about topic, partition and offset to use to move offsets
   */
  void acknowledge(Collection<ConsumerRecord<String, T>> messages);

  /**
   * Move fetchOffset of the message topic and partition to the position next after message offset.
   *
   * fetchOffset is used on the next poll request to Kafka broker.
   *
   * This method must be executed only within the consumer listener thread otherwise ConcurrentModificationException will be thrown.
   *
   * @param message - object containing information about topic, partition and offset to use to move internal fetchOffset
   */
  void seek(ConsumerRecord<String, T> message);

  /**
   * Commit the specified offsets belonging to the passed messages collection for the specified list of topics and partitions to Kafka.
   *
   * This method must be executed only within the consumer listener thread otherwise ConcurrentModificationException will be thrown.
   *
   * This method is intended to be used only in case of async processing of the messages,
   * i.e. in case when  consumed messages are handled to some processor thread via blocking queue.
   * Think twice before using it!
   *
   * @param messages - collection of objects containing information about topic, partition and offset to use to move committedOffset
   */
  void commit(Collection<ConsumerRecord<String, T>> messages);
}
