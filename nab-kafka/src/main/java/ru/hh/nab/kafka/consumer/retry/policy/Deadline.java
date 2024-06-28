package ru.hh.nab.kafka.consumer.retry.policy;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import ru.hh.nab.kafka.consumer.retry.MessageProcessingHistory;
import ru.hh.nab.kafka.consumer.retry.RetryPolicy;

public record Deadline(RetryPolicy base, Instant deadline) implements RetryPolicy {
  public Deadline(RetryPolicy base, Instant deadline) {
    this.base = Objects.requireNonNull(base);
    this.deadline = Objects.requireNonNull(deadline);
  }

  @Override
  public Optional<Instant> getNextRetryTime(MessageProcessingHistory history) {
    return base
        .getNextRetryTime(history)
        .filter(deadline::isAfter);
  }
}
