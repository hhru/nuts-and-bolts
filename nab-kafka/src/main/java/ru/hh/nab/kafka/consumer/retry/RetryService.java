package ru.hh.nab.kafka.consumer.retry;

import java.time.Instant;
import java.util.Optional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import static ru.hh.nab.kafka.consumer.retry.Headers.numberOfFails;

public interface RetryService<T> {

  void retry(ConsumerRecord<String, T> message, Instant retryTime);

  default void retry(ConsumerRecord<String, T> message, Throwable error) {
    var retryPolicy = getRetryPolicy(message, error);
    var processingHistory = getProcessingHistory(message).orElse(null);
    retry(message, retryPolicy.getNextRetryTime(processingHistory));
  }

  RetryPolicy getRetryPolicy(ConsumerRecord<String, T> message, Throwable error);

  default Optional<MessageProcessingHistory> getProcessingHistory(ConsumerRecord<String, T> message) {
    return numberOfFails(message)
        .map(
            numberOfFails -> new MessageProcessingHistory(
                Headers.creationTime(message).orElse(null),
                numberOfFails,
                Headers.lastFailTime(message).orElse(null))
        );
  }
}
