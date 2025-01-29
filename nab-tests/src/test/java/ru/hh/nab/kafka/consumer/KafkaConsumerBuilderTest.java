package ru.hh.nab.kafka.consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.hh.nab.kafka.consumer.retry.RetryPolicyResolver;
import ru.hh.nab.kafka.exception.ConfigurationException;
import ru.hh.nab.kafka.producer.KafkaProducer;
import ru.hh.nab.testbase.kafka.NoopKafkaConsumerFactory;

public class KafkaConsumerBuilderTest extends KafkaConsumerTestbase {
  public static final ConsumeStrategy<Object> CONSUME_STRATEGY = (messages, ack) -> {
  };

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
    assertThrows(ConfigurationException.class, () -> consumerFactory
        .builder(null, Object.class)
        .withConsumeStrategy(CONSUME_STRATEGY)
        .build());
  }

  @Test
  void failToBuildConsumerWithoutMessageClass() {
    assertThrows(ConfigurationException.class, () -> consumerFactory
        .builder(topicName, null)
        .withConsumeStrategy(CONSUME_STRATEGY)
        .build());
  }

  @Test
  void failToBuildConsumerWithoutConsumeStrategy() {
    assertThrows(ConfigurationException.class, () -> consumerFactory
        .builder(topicName, Object.class)
        .build());
  }

  /**
   * Verifies that retries can't be used by a consumer without consumer group
   */

  @Test
  void failToBuildConsumerWithRetriesAndWithoutConsumerGroup() {
    assertThrows(ConfigurationException.class, () -> consumerFactory
        .builder(topicName, Object.class)
        // With retries
        .withRetries(Mockito.mock(KafkaProducer.class), RetryPolicyResolver.never())
        // Without consumer group
        .withAllPartitionsAssigned(SeekPosition.EARLIEST)
        .withConsumeStrategy(CONSUME_STRATEGY)
        .build());
  }

  @Test
  void successfulBuildAndStartNoopConsumer() {
    assertDoesNotThrow(() -> new NoopKafkaConsumerFactory()
        .builder(null, null)
        .build()
        .start());
  }
}
