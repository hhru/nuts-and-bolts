package ru.hh.nab.kafka.consumer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.KafkaException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KafkaInternalTopicAckTest {
  static final ConsumerRecord<String, String> CONSUMER_RECORD_0 = new ConsumerRecord<>("", 0, 0, null, null);
  static final ConsumerRecord<String, String> CONSUMER_RECORD_1 = new ConsumerRecord<>("", 0, 1, null, null);
  static final ConsumerRecord<String, String> CONSUMER_RECORD_2 = new ConsumerRecord<>("", 1, 0, null, null);
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
    when(retryService.retry(any(), any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException()));
    ack = new KafkaInternalTopicAck<>(kafkaConsumer, nativeKafkaConsumer, retryService);
    ack.retry(CONSUMER_RECORD_0, new RuntimeException());
  }

  @Test
  void acknowledgeAllMessages() {
    assertThrows(KafkaException.class, () -> ack.acknowledge());
    verify(nativeKafkaConsumer, never()).commitSync();
    verify(nativeKafkaConsumer, never()).commitSync(anyMap());
  }

  @Test
  void acknowledgeOneMessage() {
    assertThrows(KafkaException.class, () -> ack.acknowledge(CONSUMER_RECORD_0));
    verify(nativeKafkaConsumer, never()).commitSync();
    verify(nativeKafkaConsumer, never()).commitSync(anyMap());

  }

  @Test
  void acknowledgeSomeMessages() {
    assertThrows(KafkaException.class, () -> ack.acknowledge(SOME_CONSUMER_RECORDS));
    verify(nativeKafkaConsumer, never()).commitSync();
    verify(nativeKafkaConsumer, never()).commitSync(anyMap());
  }

  @Test
  void commit() {
    assertThrows(KafkaException.class, () -> ack.commit(SOME_CONSUMER_RECORDS));
    verify(nativeKafkaConsumer, never()).commitSync();
    verify(nativeKafkaConsumer, never()).commitSync(anyMap());
  }

  @Test
  void seek() {
    assertThrows(KafkaException.class, () -> ack.seek(CONSUMER_RECORD_0));
    verify(nativeKafkaConsumer, never()).commitSync();
    verify(nativeKafkaConsumer, never()).commitSync(anyMap());
  }
}
