package ru.hh.nab.kafka.consumer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class KafkaInternalTopicAckTest {
  static final ConsumerRecord<String, String> CONSUMER_RECORD_0 = new ConsumerRecord<>("t", 0, 0, null, null);
  static final ConsumerRecord<String, String> CONSUMER_RECORD_1 = new ConsumerRecord<>("t", 0, 1, null, null);
  static final ConsumerRecord<String, String> CONSUMER_RECORD_2 = new ConsumerRecord<>("t", 1, 10, null, null);
  static final List<ConsumerRecord<String, String>> ALL_CONSUMER_RECORDS = List.of(CONSUMER_RECORD_0, CONSUMER_RECORD_1, CONSUMER_RECORD_2);
  static final List<ConsumerRecord<String, String>> SOME_CONSUMER_RECORDS = List.of(CONSUMER_RECORD_0, CONSUMER_RECORD_2);

  ConsumerConsumingState<String> consumingState;
  @Mock
  KafkaConsumer<String> kafkaConsumer;
  @Mock
  Consumer<?, ?> nativeKafkaConsumer;
  @Mock
  RetryService<String> retryService;
  KafkaInternalTopicAck<String> ack;

  @BeforeEach
  void setUp() {
    consumingState = new ConsumerConsumingState<>();
    consumingState.prepareForNextBatch(ALL_CONSUMER_RECORDS);
    when(kafkaConsumer.getConsumingState()).thenReturn(consumingState);
    ack = new KafkaInternalTopicAck<>(kafkaConsumer, nativeKafkaConsumer, retryService);
  }

  public Stream<Executable> acknowledgeMethods() {
    return Stream.of(
        () -> ack.acknowledge(),
        () -> ack.acknowledge(CONSUMER_RECORD_0),
        () -> ack.acknowledge(SOME_CONSUMER_RECORDS),
        () -> ack.commit(SOME_CONSUMER_RECORDS),
        () -> ack.seek(CONSUMER_RECORD_0)
    );
  }

  @ParameterizedTest
  @MethodSource("acknowledgeMethods")
  void failOnFailedRetry(Executable executable) {
    when(retryService.retry(any(), any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException()));
    ack.retry(CONSUMER_RECORD_0, new RuntimeException());
    assertThrows(KafkaException.class, executable);
    verify(nativeKafkaConsumer, never()).commitSync();
    verify(nativeKafkaConsumer, never()).commitSync(anyMap());
  }

  @ParameterizedTest
  @MethodSource("acknowledgeMethods")
  void waitForRetryToComplete(Executable executable) {
    CompletableFuture<Void> retryFuture = CompletableFuture.runAsync(
        () -> {},
        CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS)
    );
    doReturn(retryFuture).when(retryService).retry(any(), any());
    ack.retry(CONSUMER_RECORD_0, new RuntimeException());
    assertFalse(retryFuture.isDone());
    assertDoesNotThrow(executable);
    assertTrue(retryFuture.isDone());
  }

  @Test
  void moveSeekedOffsetsOnNextSeekAfterRetry() {
    when(retryService.retry(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    ack.acknowledge(CONSUMER_RECORD_0);
    assertEquals(1, consumingState.getBatchSeekedOffsets().get(new TopicPartition("t", 0)).offset());
    ack.retry(CONSUMER_RECORD_1, new RuntimeException());
    assertEquals(1, consumingState.getBatchSeekedOffsets().get(new TopicPartition("t", 0)).offset());
    ack.seek(CONSUMER_RECORD_2);
    assertEquals(2, consumingState.getBatchSeekedOffsets().get(new TopicPartition("t", 0)).offset());
    assertEquals(11, consumingState.getBatchSeekedOffsets().get(new TopicPartition("t", 1)).offset());
  }
}
