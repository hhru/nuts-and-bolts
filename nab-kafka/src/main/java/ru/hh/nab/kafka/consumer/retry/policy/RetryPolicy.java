package ru.hh.nab.kafka.consumer.retry.policy;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import ru.hh.nab.kafka.consumer.retry.MessageProcessingHistory;
import ru.hh.nab.kafka.consumer.retry.RetryPolicyResolver;

/**
 * Decides when (and if) a message should be retried based on its {@link MessageProcessingHistory}
 *
 * @see RetryPolicyResolver
 */
public sealed interface RetryPolicy permits Deadline, Fixed, Never, Progressive, RetryLimit, Ttl {

  /**
   * Creates policy that never retries
   */
  static Never never() {
    return new Never();
  }

  /**
   * Creates policy that always schedules retry at given <b>delay</b> after {@link MessageProcessingHistory#lastFailTime()}.
   * <p>
   * Number and/or duration of retries may be limited by applying any combination of <b>with*</b> modifiers
   *
   * @see #withRetryLimit(long)
   * @see #withTtl(Duration)
   * @see #withDeadline(Instant)
   */
  static Fixed fixed(Duration delay) {
    return new Fixed(delay);
  }

  /**
   * Creates policy that schedules retry after delay that <b>delayByRetryNumber</b> returns for {@link MessageProcessingHistory#retryNumber()}
   *
   * @see #withRetryLimit(long)
   * @see #withTtl(Duration)
   * @see #withDeadline(Instant)
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
   * that is, return value of {@link #getNextRetryTime(MessageProcessingHistory)} is after <b>deadline</b>
   */
  default Deadline withDeadline(Instant deadline) {
    return new Deadline(this, deadline);
  }

  /**
   * Creates modified policy that is based on this policy but stops retries when given TTL is reached for the message,
   * that is, return value of {@link #getNextRetryTime(MessageProcessingHistory)}
   * is after {@link MessageProcessingHistory#creationTime()} plus <b>ttl</b>
   */
  default Ttl withTtl(Duration ttl) {
    return new Ttl(this, ttl);
  }

  /**
   * @param history Message processing history (required)
   * @return Time when this message should be retried next. {@link Optional#empty()} means message should not be retried anymore
   */
  Optional<Instant> getNextRetryTime(MessageProcessingHistory history);

}
