package ru.hh.nab.kafka.consumer.retry.policy;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import ru.hh.nab.kafka.consumer.retry.MessageProcessingHistory;

public record RetryLimit(RetryPolicy base, long limit) implements RetryPolicy {
  public RetryLimit(RetryPolicy base, long limit) {
    this.base = Objects.requireNonNull(base);
    if (limit < 0) {
      throw new IllegalArgumentException("Limit should be positive");
    }
    else if (limit == 0) {
      throw new IllegalArgumentException("Explicitly use Never policy instead of limit 0");
    }
    this.limit = limit;
  }

  @Override
  public Optional<Instant> getNextRetryTime(MessageProcessingHistory history) {
    return history.retryNumber() > limit ? Optional.empty() : base.getNextRetryTime(history);
  }
}
