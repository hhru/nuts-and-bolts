package ru.hh.nab.kafka.consumer.retry.policy;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import ru.hh.nab.kafka.consumer.retry.MessageProcessingHistory;
import ru.hh.nab.kafka.consumer.retry.RetryPolicy;

public record FixedDelay(Duration delay) implements RetryPolicy {
  public FixedDelay(Duration delay) {
    if (delay.isNegative()) {
      throw new IllegalArgumentException("Delay should be positive");
    } else if (delay.isZero()) {
      throw new IllegalArgumentException("Explicitly use Never policy instead of zero delay");
    }
    this.delay = delay;
  }

  @Override
  public Optional<Instant> getNextRetryTime(MessageProcessingHistory history) {
    return Optional.of(history
        .lastFailTime()
        .plus(delay));
  }
}
