package ru.hh.nab.kafka.consumer.retry.policy;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import ru.hh.nab.kafka.consumer.retry.MessageProcessingHistory;

public record Progressive(DelayByRetryNumber delayByRetryNumber) implements RetryPolicy {
  public Progressive(DelayByRetryNumber delayByRetryNumber) {
    this.delayByRetryNumber = Objects.requireNonNull(delayByRetryNumber);
  }

  @Override
  public Optional<Instant> getNextRetryTime(MessageProcessingHistory history) {
    return Optional.of(history
        .lastFailTime()
        .plus(delayByRetryNumber.getDelay(history.retryNumber())));
  }

  @FunctionalInterface
  public interface DelayByRetryNumber {
    Duration getDelay(long retryNumber);
  }
}
