package ru.hh.nab.kafka.consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

public class KafkaConsumerBuilderTest extends KafkaConsumerTestBase {
  public static final ConsumeStrategy<Object> CONSUME_STRATEGY = (messages, ack) -> {};

  @Test
  void successfulBuildAndStartWithOnlyRequiredParams() {
    assertDoesNotThrow(() -> consumerFactory
        .builder(topicName, Object.class)
        .withConsumeStrategy(CONSUME_STRATEGY)
        .build()
        .start());
  }

  @Test
  void failToBuildConsumerWithoutTopic() {
    assertThrows(NullPointerException.class, () -> consumerFactory
        .builder(null, Object.class)
        .withConsumeStrategy(CONSUME_STRATEGY)
        .build());
  }

  @Test
  void failToBuildConsumerWithoutMessageClass() {
    assertThrows(NullPointerException.class, () -> consumerFactory
        .builder(topicName, null)
        .withConsumeStrategy(CONSUME_STRATEGY)
        .build());
  }

  @Test
  void failToBuildConsumerWithoutConsumeStrategy() {
    assertThrows(NullPointerException.class, () -> consumerFactory
        .builder(topicName, Object.class)
        .build());
  }
}
