package ru.hh.nab.kafka.consumer.retry.policy;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.hh.nab.kafka.consumer.retry.MessageProcessingHistory;
import static ru.hh.nab.kafka.consumer.retry.policy.RetryPolicy.fixed;
import static ru.hh.nab.kafka.consumer.retry.policy.RetryPolicy.never;
import static ru.hh.nab.kafka.consumer.retry.policy.RetryPolicy.progressive;

class RetryPolicyTest {

  static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MINUTES);
  static final Instant START = NOW.minusSeconds(1);

  public static Stream<Arguments> policyTestData() {
    return Stream.of(
        arguments(never(), history(1), null),
        arguments(never(), history(10), null),
        arguments(never().withRetryLimit(10), history(1), null),
        arguments(never().withDeadline(NOW.plusSeconds(1000)), history(1), null),
        arguments(never().withTtl(seconds(1000)), history(1), null),

        arguments(fixed(seconds(10)), history(1, NOW), NOW.plusSeconds(10)),
        arguments(fixed(seconds(20)), history(100, NOW), NOW.plusSeconds(20)),

        arguments(fixed(seconds(10)).withRetryLimit(10), history(10), NOW.plusSeconds(10)),
        arguments(fixed(seconds(10)).withRetryLimit(10), history(11), null),

        arguments(fixed(seconds(10)).withDeadline(NOW.plusSeconds(11)), history(1), NOW.plusSeconds(10)),
        arguments(fixed(seconds(10)).withDeadline(NOW.plusSeconds(10)), history(1), null),

        arguments(fixed(seconds(10)).withTtl(seconds(12)), history(1), NOW.plusSeconds(10)),
        arguments(fixed(seconds(10)).withTtl(seconds(11)), history(1), null),

        arguments(
            fixed(seconds(10)).withRetryLimit(10).withDeadline(NOW.plusSeconds(50)),
            history(4, NOW.plusSeconds(30)),
            NOW.plusSeconds(40)
        ),
        arguments(
            fixed(seconds(10)).withRetryLimit(10).withDeadline(NOW.plusSeconds(50)),
            history(5, NOW.plusSeconds(40)),
            null
        ),

        arguments(
            fixed(seconds(10)).withRetryLimit(10).withTtl(seconds(20)),
            history(1),
            NOW.plusSeconds(10)
        ),
        arguments(
            fixed(seconds(10)).withRetryLimit(10).withTtl(seconds(20)),
            history(2, NOW.plusSeconds(10)),
            null
        ),

        arguments(progressive(retryNumber -> retryNumber <= 10 ? seconds(retryNumber) : seconds(10)), history(1, NOW), NOW.plusSeconds(1)),
        arguments(progressive(retryNumber -> retryNumber <= 10 ? seconds(retryNumber) : seconds(10)), history(9, NOW), NOW.plusSeconds(9)),
        arguments(progressive(retryNumber -> retryNumber <= 10 ? seconds(retryNumber) : seconds(10)), history(10, NOW), NOW.plusSeconds(10)),
        arguments(progressive(retryNumber -> retryNumber <= 10 ? seconds(retryNumber) : seconds(10)), history(100, NOW), NOW.plusSeconds(10))
    );
  }

  static MessageProcessingHistory history(long retryNumber) {
    return history(retryNumber, NOW);
  }

  static MessageProcessingHistory history(long retryNumber, Instant lastFailTime) {
    return history(START, retryNumber, lastFailTime);
  }

  static MessageProcessingHistory history(Instant creationTime, long retryNumber, Instant lastFailTime) {
    return new MessageProcessingHistory(creationTime, retryNumber, lastFailTime);
  }

  static Duration seconds(long number) {
    return Duration.ofSeconds(number);
  }

  @ParameterizedTest
  @MethodSource("policyTestData")
  void testNextRetryTime(RetryPolicy policy, MessageProcessingHistory history, Instant expected) {
    assertEquals(Optional.ofNullable(expected), policy.getNextRetryTime(history));
  }
}
