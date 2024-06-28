package ru.hh.nab.kafka.consumer.retry;

import java.time.Duration;
import java.time.Instant;

@FunctionalInterface
public interface RetryPolicy {
  static RetryPolicy fixedDelay(Duration delay) {
    return history -> history.lastFailTime().toInstant().plus(delay);
  }

  Instant getNextRetryTime(MessageProcessingHistory history);
}
