package ru.hh.nab.kafka.consumer;

import java.util.Collection;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import ru.hh.nab.kafka.consumer.retry.RetryPolicyResolver;
import ru.hh.nab.kafka.consumer.retry.policy.RetryPolicy;
import ru.hh.nab.kafka.producer.KafkaProducer;

public interface Ack<T> {

  /**
   * Move committedOffset and fetchOffset to the offsets returned on the last Consumer.poll() for the subscribed list of topics and partitions
   * <p>
   * fetchOffset is used on the next poll request to Kafka broker.
   * committedOffset is used in case if the node goes down, or in case if re-balancing happened.
   * <p>
   * This method must be executed only within the consumer listener thread.
   * It blocks thread until both offsets have been committed to kafka and all retry futures for current batch have been completed.
   */
  void acknowledge();

  /**
   * Move both Kafka committedOffset and fetchOffset
   * belonging to the topic and partition of the passed record
   * <p>
   * fetchOffset is used on the next poll request to Kafka broker.
   * committedOffset is used in case if the node goes down, or in case if re-balancing happened.
   * <p>
   * This method must be executed only within the consumer listener thread otherwise ConcurrentModificationException will be thrown.
   * It blocks thread until both offsets have been committed to kafka and all retry futures for current batch have been completed.
   *
   * @param message - object containing information about topic, partition and offset to use to move offsets
   */
  void acknowledge(ConsumerRecord<String, T> message);

  /**
   * A non-blocking asynchronous operation which sends message into a separate Dead Letter Queue (DLQ) topic.
   * This method should only be called within the consumer listener thread to avoid ConcurrentModificationException.
   *
   * @implNote Result of this operation may be available at some time in the future or as part of
   * {@link Ack#acknowledge()}/{@link Ack#acknowledge(ConsumerRecord)}/{@link Ack#acknowledge(Collection)}/{@link Ack#seek(ConsumerRecord)}}
   * invocation
   * @param message - an object containing information about the topic, partition, and offset to be used for moving the offsets
   * @see ConsumerBuilder#withDlq(String, KafkaProducer)
   */
  void nAck(ConsumerRecord<String, T> message);


  /**
   * Move both Kafka committedOffset and fetchOffset
   * belonging to the topic and partition of the passed record
   * <p>
   * fetchOffset is used on the next poll request to Kafka broker.
   * committedOffset is used in case if the node goes down, or in case if re-balancing happened.
   * <p>
   * This method must be executed only within the consumer listener thread otherwise ConcurrentModificationException will be thrown.
   * It blocks thread until both offsets have been committed to kafka and all retry futures for current batch have been completed.
   *
   * @param messages - collection of objects containing information about topic, partition and offset to use to move offsets
   */
  void acknowledge(Collection<ConsumerRecord<String, T>> messages);

  /**
   * A non-blocking asynchronous operation which sends messages into a separate Dead Letter Queue (DLQ) topic.
   * This method should only be called within the consumer listener thread to avoid ConcurrentModificationException.
   *
   * @implNote Result of this operation may be available at some time in the future or as part of
   * {@link Ack#acknowledge()}/{@link Ack#acknowledge(ConsumerRecord)}/{@link Ack#acknowledge(Collection)}/{@link Ack#seek(ConsumerRecord)}}
   * invocation
   * @param messages - a collection of objects containing information about the topic, partition, and offset to be used for moving the offsets
   * @see ConsumerBuilder#withDlq(String, KafkaProducer)
   */
  void nAck(Collection<ConsumerRecord<String, T>> messages);

  /**
   * Move fetchOffset of the message topic and partition to the position next after message offset.
   * <p>
   * fetchOffset is used on the next poll request to Kafka broker.
   * <p>
   * This method must be executed only within the consumer listener thread otherwise ConcurrentModificationException will be thrown.
   * It blocks thread until both offsets have been committed to kafka and all retry futures for current batch have been completed.
   *
   * @param message - object containing information about topic, partition and offset to use to move internal fetchOffset
   */
  void seek(ConsumerRecord<String, T> message);

  /**
   * Commit the specified offsets belonging to the passed messages collection for the specified list of topics and partitions to Kafka.
   * <p>
   * This method must be executed only within the consumer listener thread otherwise ConcurrentModificationException will be thrown.
   * <p>
   * This method is intended to be used only in case of async processing of the messages,
   * i.e. in case when  consumed messages are handled to some processor thread via blocking queue.
   * Think twice before using it!
   *
   * @param messages - collection of objects containing information about topic, partition and offset to use to move committedOffset
   */
  void commit(Collection<ConsumerRecord<String, T>> messages);

  /**
   * Schedule message for retry because of processing error or business logic decision.
   * <p>
   * Time of retry is determined by consumer configuration.
   * <p>
   * Caller MAY use returned Future but is not required to wait for it to complete because
   * next call to {@link #seek(ConsumerRecord)} or any of acknowledge(...) methods will do that.
   *
   * @param message Message that should be scheduled for retry
   * @param error Exception that was the cause for retrying message. If there was no exception set to {@code null}
   *              or create custom exception with details that Ack needs to determine retry time correctly
   * @return Future that represents an async operation of scheduling the message for retry
   * @throws UnsupportedOperationException if implementation does not support retries
   * @see ConsumerBuilder#withRetries(KafkaProducer, RetryPolicyResolver)
   * @see RetryPolicyResolver
   * @see RetryPolicy
   */
  void retry(ConsumerRecord<String, T> message, Throwable error);
}
