package ru.hh.nab.kafka.consumer.retry.policy;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import ru.hh.nab.kafka.consumer.retry.MessageProcessingHistory;

public record Ttl(RetryPolicy base, Duration ttl) implements RetryPolicy {
  public Ttl(RetryPolicy base, Duration ttl) {
    this.base = Objects.requireNonNull(base);
    if (ttl.isNegative()) {
      throw new IllegalArgumentException("Ttl should be positive");
    } else if (ttl.isZero()) {
      throw new IllegalArgumentException("Explicitly use Never policy instead of zero ttl");
    }
    this.ttl = ttl;
  }

  @Override
  public Optional<Instant> getNextRetryTime(MessageProcessingHistory history) {
    return base
        .getNextRetryTime(history)
        .filter(history
            .creationTime()
            .plus(ttl)::isAfter);
  }

  @Override
  public boolean hasFixedDelay() {
    return base.hasFixedDelay();
  }
}
