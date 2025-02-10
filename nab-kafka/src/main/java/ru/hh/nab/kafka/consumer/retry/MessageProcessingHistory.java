package ru.hh.nab.kafka.consumer.retry;

import java.time.Instant;
import java.util.Objects;

/**
 * Message processing history. History is not meant to be created before first failure,
 * so all fields are required, and retryNumber should be greater than zero
 *
 * @param creationTime Time when this message was initially created
 * @param retryNumber  Number of failures to process this message
 * @param lastFailTime Time of last processing failure for this message
 */
public record MessageProcessingHistory(
    Instant creationTime,
    long retryNumber,
    Instant lastFailTime) {
  public MessageProcessingHistory(Instant creationTime, long retryNumber, Instant lastFailTime) {
    this.creationTime = Objects.requireNonNull(creationTime);
    if (retryNumber < 1) {
      throw new IllegalArgumentException("retryNumber should not be negative");
    }
    this.retryNumber = retryNumber;
    this.lastFailTime = Objects.requireNonNull(lastFailTime);
  }

  public static MessageProcessingHistory initial(Instant creationTime, Instant lastFailTime) {
    return new MessageProcessingHistory(creationTime, 1, lastFailTime);
  }

  public MessageProcessingHistory withOneMoreFail(Instant failTime) {
    return new MessageProcessingHistory(creationTime, retryNumber + 1, failTime);
  }
}
