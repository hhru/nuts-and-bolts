package ru.hh.nab.kafka.consumer.retry.policy;

import java.time.Instant;
import java.util.Optional;
import ru.hh.nab.kafka.consumer.retry.MessageProcessingHistory;
import ru.hh.nab.kafka.consumer.retry.RetryPolicy;

public record Never() implements RetryPolicy {
  @Override
  public Optional<Instant> getNextRetryTime(MessageProcessingHistory history) {
    return Optional.empty();
  }
}
