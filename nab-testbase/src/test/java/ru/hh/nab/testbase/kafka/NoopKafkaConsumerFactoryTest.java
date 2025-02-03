package ru.hh.nab.testbase.kafka;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.Test;

public class NoopKafkaConsumerFactoryTest {

  @Test
  public void successfulBuildAndStartNoopConsumer() {
    assertDoesNotThrow(() -> new NoopKafkaConsumerFactory()
        .builder(null, null)
        .build()
        .start());
  }
}
