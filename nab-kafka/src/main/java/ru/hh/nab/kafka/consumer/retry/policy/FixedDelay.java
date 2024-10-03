package ru.hh.nab.kafka.consumer.retry.policy;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import ru.hh.nab.kafka.consumer.retry.MessageProcessingHistory;

public record FixedDelay(Duration delay) implements RetryPolicy {
  public FixedDelay {
    if (delay.isNegative()) {
      throw new IllegalArgumentException("Delay should be positive");
    }
    if (delay.isZero()) {
      throw new IllegalArgumentException("Explicitly use Never policy instead of zero delay");
    }
  }

  @Override
  public Optional<Instant> getNextRetryTime(MessageProcessingHistory history) {
    return Optional.of(history
        .lastFailTime()
        .plus(delay));
  }

  @Override
  public boolean hasFixedDelay() {
    return true;
  }
}
