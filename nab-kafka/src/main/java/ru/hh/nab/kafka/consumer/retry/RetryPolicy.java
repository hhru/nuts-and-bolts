package ru.hh.nab.kafka.consumer.retry;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import ru.hh.nab.kafka.consumer.retry.policy.Deadline;
import ru.hh.nab.kafka.consumer.retry.policy.FixedDelay;
import ru.hh.nab.kafka.consumer.retry.policy.Never;
import ru.hh.nab.kafka.consumer.retry.policy.Progressive;
import ru.hh.nab.kafka.consumer.retry.policy.RetryLimit;
import ru.hh.nab.kafka.consumer.retry.policy.Ttl;

@FunctionalInterface
public interface RetryPolicy {

  /**
   * Creates policy that never retries
   */
  static Never never() {
    return new Never();
  }

  /**
   * Creates policy that always decides to schedule retry at given <b>delay</b> after {@link MessageProcessingHistory#lastFailTime()}
   */
  static FixedDelay fixedDelay(Duration delay) {
    return new FixedDelay(delay);
  }

  /**
   * Creates policy that schedules retry after delay that <b>delays</b> map holds for key {@link MessageProcessingHistory#retryNumber()}.
   * If there is no value for retryNumber than <b>defaultDelay</b> is used
   */
  static Progressive progressive(Progressive.DelayByRetryNumber delayByRetryNumber) {
    return new Progressive(delayByRetryNumber);
  }

  /**
   * Creates modified policy that is based on this policy but stops retries after given number of attempts,
   * that is, {@link MessageProcessingHistory#retryNumber()} is greater than  <b>limit</b>
   */
  default RetryLimit withRetryLimit(long limit) {
    return new RetryLimit(this, limit);
  }

  /**
   * Creates modified policy that is based on this policy but stops retries after given deadline,
   * that is, return value of this.getNextRetryTime() is after <b>deadline</b>
   */
  default Deadline withDeadline(Instant deadline) {
    return new Deadline(this, deadline);
  }

  /**
   * Creates modified policy that is based on this policy but stops retries when given TTL is reached for the message,
   * that is, return value of this.getNextRetryTime() is after {@link MessageProcessingHistory#creationTime()} plus <b>ttl</b>
   */
  default Ttl withTtl(Duration ttl){
    return new Ttl(this, ttl);
  }

  /**
   * @param history Message processing history (required)
   * @return Time when this message should be retried next. {@link Optional#empty()} means message should not be retried anymore
   */
  Optional<Instant> getNextRetryTime(MessageProcessingHistory history);
}
