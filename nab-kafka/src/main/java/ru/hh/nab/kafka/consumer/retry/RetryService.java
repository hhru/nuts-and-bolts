package ru.hh.nab.kafka.consumer.retry;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.consumer.ConsumerRecord;


/**
 * Provides retries implementation and message processing history to {@link ru.hh.nab.kafka.consumer.Ack}.
 * This is not a public API, it is not meant to be implemented outside NAB code, and it may change any time without backward compatibility
 */
public abstract sealed class RetryService<T> permits KafkaRetryService {

  public CompletableFuture<?> retry(ConsumerRecord<String, T> message, Throwable error) {
    RetryPolicy retryPolicy = getRetryPolicy(message, error);
    MessageProcessingHistory updatedProcessingHistory = getProcessingHistory(message)
        .map(MessageProcessingHistory::withOneMoreFail)
        .orElseGet(() -> createInitialProcessingHistory(message));
    return retryPolicy
        .getNextRetryTime(updatedProcessingHistory)
        .map(nextRetryTime -> retry(message, nextRetryTime, updatedProcessingHistory))
        .orElseGet(() -> CompletableFuture.completedFuture(null));
  }

  protected abstract CompletableFuture<?> retry(
      ConsumerRecord<String, T> message,
      Instant retryTime,
      MessageProcessingHistory updatedProcessingHistory);

  public abstract Optional<MessageProcessingHistory> getProcessingHistory(ConsumerRecord<String, T> message);

  protected MessageProcessingHistory createInitialProcessingHistory(ConsumerRecord<String, T> message) {
    return new MessageProcessingHistory(Instant.ofEpochMilli(message.timestamp()), 1, Instant.now());
  }

  protected abstract RetryPolicy getRetryPolicy(ConsumerRecord<String, T> message, Throwable error);
}
