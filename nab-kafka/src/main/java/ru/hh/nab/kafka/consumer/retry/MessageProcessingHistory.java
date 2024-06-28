package ru.hh.nab.kafka.consumer.retry;

import java.time.OffsetDateTime;

public record MessageProcessingHistory(
    OffsetDateTime creationTime,
    long numberOfFails,
    OffsetDateTime lastFailTime) {

  public MessageProcessingHistory withOneMoreFail() {
    return withOneMoreFail(OffsetDateTime.now());
  }

  public MessageProcessingHistory withOneMoreFail(OffsetDateTime failTime) {
    return new MessageProcessingHistory(creationTime, numberOfFails + 1, failTime);
  }
}
