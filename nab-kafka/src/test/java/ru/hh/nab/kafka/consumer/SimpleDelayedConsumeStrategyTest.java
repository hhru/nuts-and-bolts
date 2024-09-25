package ru.hh.nab.kafka.consumer;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SimpleDelayedConsumeStrategyTest {
  private static final Instant NOW = Instant.now();
  private static final Function<ConsumerRecord<String, Long>, Instant> GET_READY_TIME =
      message -> Instant.ofEpochMilli(message.value());

  @Mock
  private Ack<Long> myAck;
  private List<ConsumerRecord<String, Long>> processedMessages;
  private final SimpleDelayedConsumeStrategy<Long> strategy = new SimpleDelayedConsumeStrategy<>(
      (messages, ack) -> {
        processedMessages.addAll(messages);
        ack.acknowledge();
      },
      GET_READY_TIME,
      Duration.ofSeconds(2),
      Clock.fixed(NOW, ZoneId.systemDefault())
  );

  @BeforeEach
  void setUp() {
    processedMessages = new ArrayList<>();
  }

  @Test
  void sleepWhenNoReadyMessages() {
    assertTimeout(Duration.ofSeconds(3), () -> strategy.onMessagesBatch(
        messages(
            NOW.toEpochMilli() - 2,
            NOW.toEpochMilli() - 1
        ),
        myAck
    ));
    assertEquals(0, processedMessages.size());
    verify(myAck, never()).acknowledge();
    verify(myAck, never()).acknowledge(anyCollection());
  }

  @Test
  void processReadyMessages() {
    assertTimeout(Duration.ofSeconds(1), () -> strategy.onMessagesBatch(
        messages(
            NOW.toEpochMilli() - 2,
            NOW.toEpochMilli() - 1,
            NOW.toEpochMilli() + 1
        ),
        myAck
    ));
    assertEquals(1, processedMessages.size());
    verify(myAck, never()).acknowledge();
    verify(myAck, only()).acknowledge(eq(processedMessages));
  }

  @Test
  void processAllMessages() {
    assertTimeout(Duration.ofSeconds(1), () -> strategy.onMessagesBatch(
        messages(
            NOW.toEpochMilli() + 1,
            NOW.toEpochMilli() + 2,
            NOW.toEpochMilli() + 3

        ),
        myAck
    ));
    assertEquals(3, processedMessages.size());
    verify(myAck, never()).acknowledge();
    verify(myAck, only()).acknowledge(eq(processedMessages));
  }

  private static List<ConsumerRecord<String, Long>> messages(Long... messages) {
    return Stream
        .of(messages)
        .map(SimpleDelayedConsumeStrategyTest::message)
        .toList();
  }

  private static ConsumerRecord<String, Long> message(Long value) {
    return new ConsumerRecord<>("", 0, value, null, value);
  }
}
