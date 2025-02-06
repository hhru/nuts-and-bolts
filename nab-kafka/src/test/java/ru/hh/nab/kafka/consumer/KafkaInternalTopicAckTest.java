package ru.hh.nab.kafka.consumer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetCommitCallback;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.UnknownLeaderEpochException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class KafkaInternalTopicAckTest {
  private static final Random RANDOM = new Random();

  /**
   * Verifies all messages are acknowledged and offset moved further
   */

  @Test
  void acknowledge() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Test
    ack.acknowledge();

    Mockito.verify(nativeConsumer, Mockito.times(1)).commitSync();
    Assertions.assertTrue(consumer.getConsumerContext().isWholeBatchAcked());
  }

  /**
   * Verifies all messages are NOT acknowledged and offset NOT moved further
   */

  @Test
  void acknowledgeException() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    Mockito.doThrow(new KafkaException("Stub exception")).when(nativeConsumer).commitSync();
    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Test
    Assertions.assertThrows(KafkaException.class, () -> ack.acknowledge());

    Assertions.assertFalse(consumer.getConsumerContext().isWholeBatchAcked());
  }

  /**
   * Verifies all messages are acknowledged, offset moved further and retries completed
   */

  @Test
  void acknowledgeRetry() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    ConsumerRecord<String, String> record = createRecord();
    consumer.getConsumerContext().addRetryFuture(CompletableFuture.completedFuture(null), record);

    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Test
    ack.acknowledge();

    Assertions.assertTrue(consumer.getConsumerContext().getAllBatchRetryFuturesAsOne().isDone());
    Mockito.verify(nativeConsumer, Mockito.times(1)).commitSync();
    Assertions.assertTrue(consumer.getConsumerContext().isWholeBatchAcked());
  }

  /**
   * Verifies all messages are NOT acknowledged, offset NOT moved further and sent to DLQ
   */

  @Test
  void nAcknowledge() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Test
    ack.nAcknowledge();

    Mockito.verify(nativeConsumer, Mockito.times(1)).commitSync();
    Assertions.assertTrue(consumer.getConsumerContext().isWholeBatchAcked());
  }

  /**
   * Verifies all messages are NOT acknowledged and offset NOT moved further if in-the-middle exception happens
   */

  @Test
  void nAcknowledgeExceptionOnSecondMessage() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    // at least 3 are always created by a factory method
    ConsumerRecord<String, String> secondMessage = consumer.getConsumerContext().getCurrentBatch().get(1);

    DeadLetterQueue<String> dlqMock = consumer.getDeadLetterQueue();
    // fail on second message
    Mockito.doThrow(new KafkaException("Stub exception")).when(dlqMock).send(secondMessage);
    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Test
    Assertions.assertThrows(KafkaException.class, () -> ack.nAcknowledge());

    Assertions.assertEquals(0, consumer.getConsumerContext().getBatchSeekedOffsets().size());
    Assertions.assertFalse(consumer.getConsumerContext().isWholeBatchAcked());
  }

  /**
   * Verifies all messages are acknowledged, offset moved further and retries completed
   */

  @Test
  void nAcknowledgeRetry() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    ConsumerRecord<String, String> record = createRecord();
    consumer.getConsumerContext().addRetryFuture(CompletableFuture.completedFuture(null), record);

    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Test
    ack.nAcknowledge();

    Assertions.assertTrue(consumer.getConsumerContext().getAllBatchRetryFuturesAsOne().isDone());
    Mockito.verify(nativeConsumer, Mockito.times(1)).commitSync();
    Assertions.assertTrue(consumer.getConsumerContext().isWholeBatchAcked());
  }

  /**
   * Verifies specific message acknowledged and offset moved further
   */

  @Test
  void acknowledgeSingleMessage() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    // at least 3 are always created by a factory method
    ConsumerRecord<String, String> record = consumer.getConsumerContext().getCurrentBatch().get(0);
    TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
    // Offset must be moved to a next message in order to proceed
    OffsetAndMetadata newOffset = new OffsetAndMetadata(record.offset() + 1);

    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Test
    ack.acknowledge(record);

    Mockito.verify(nativeConsumer, Mockito.times(1)).commitSync(Map.of(topicPartition, newOffset));

    // Shared state - offsets
    Assertions.assertEquals(newOffset, consumer.getConsumerContext().getGlobalSeekedOffset(topicPartition).orElseThrow());
    Assertions.assertEquals(List.of(record).size(), consumer.getConsumerContext().getBatchSeekedOffsets().size());
    Assertions.assertEquals(newOffset, consumer.getConsumerContext().getBatchSeekedOffsets().get(topicPartition));
  }

  /**
   * Verifies specific message is NOT acknowledged and offset NOT moved further
   */

  @Test
  void acknowledgeSingleMessageException() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    // at least 3 are always created by a factory method
    ConsumerRecord<String, String> record = consumer.getConsumerContext().getCurrentBatch().get(0);
    TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
    // Offset must be moved to a next message in order to proceed
    OffsetAndMetadata newOffset = new OffsetAndMetadata(record.offset() + 1);

    Mockito.doThrow(new KafkaException("Stub exception")).when(nativeConsumer).commitSync(Map.of(topicPartition, newOffset));
    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Test
    Assertions.assertThrows(KafkaException.class, () -> ack.acknowledge(record));

    // Shared state - offsets
    Assertions.assertTrue(consumer.getConsumerContext().getGlobalSeekedOffset(topicPartition).isEmpty());
    Assertions.assertEquals(0, consumer.getConsumerContext().getBatchSeekedOffsets().size());
    Assertions.assertNull(consumer.getConsumerContext().getBatchSeekedOffsets().get(topicPartition));
  }

  /**
   * Verifies specific message is acknowledged, offset moved further and retries completed
   */

  @Test
  void acknowledgeSingleMessageRetry() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    // at least 3 are always created by a factory method
    ConsumerRecord<String, String> record = consumer.getConsumerContext().getCurrentBatch().get(0);
    TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
    // Offset must be moved to a next message in order to proceed
    OffsetAndMetadata newOffset = new OffsetAndMetadata(record.offset() + 1);

    // always completes retry operation
    Mockito
        .when(
            consumer
                .getRetryQueue()
                .retry(Mockito.any(ConsumerRecord.class), Mockito.any(Exception.class))
        )
        .thenReturn(CompletableFuture.completedFuture("Done"));
    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Business case when we were supposed to retry
    ack.retry(record, new UnknownLeaderEpochException("Stub exception"));

    // Test
    ack.acknowledge(record);

    // Verify retry awaited and passed
    Assertions.assertEquals(List.of(record).size(), consumer.getConsumerContext().getBatchRetryMessages().size());
    Assertions.assertTrue(consumer.getConsumerContext().getAllBatchRetryFuturesAsOne().isDone());

    Mockito.verify(nativeConsumer, Mockito.times(1)).commitSync(Map.of(topicPartition, newOffset));

    // Shared state - offsets
    Assertions.assertEquals(newOffset, consumer.getConsumerContext().getGlobalSeekedOffset(topicPartition).orElseThrow());
    Assertions.assertEquals(List.of(record).size(), consumer.getConsumerContext().getBatchSeekedOffsets().size());
    Assertions.assertEquals(newOffset, consumer.getConsumerContext().getBatchSeekedOffsets().get(topicPartition));
  }

  /**
   * Verifies specific message acknowledged and offset moved further
   */

  @Test
  void nAcknowledgeSingleMessage() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    // at least 3 are always created by a factory method
    ConsumerRecord<String, String> record = consumer.getConsumerContext().getCurrentBatch().get(0);
    TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
    // Offset must be moved to a next message in order to proceed
    OffsetAndMetadata newOffset = new OffsetAndMetadata(record.offset() + 1);

    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Test
    ack.nAcknowledge(record);

    Mockito.verify(nativeConsumer, Mockito.times(1)).commitSync(Map.of(topicPartition, newOffset));

    // Shared state - offsets
    Assertions.assertEquals(newOffset, consumer.getConsumerContext().getGlobalSeekedOffset(topicPartition).orElseThrow());
    Assertions.assertEquals(List.of(record).size(), consumer.getConsumerContext().getBatchSeekedOffsets().size());
    Assertions.assertEquals(newOffset, consumer.getConsumerContext().getBatchSeekedOffsets().get(topicPartition));
  }

  /**
   * Verifies specific message is NOT acknowledged and offset NOT moved further
   */

  @Test
  void nAcknowledgeSingleMessageException() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    // at least 3 are always created by a factory method
    ConsumerRecord<String, String> record = consumer.getConsumerContext().getCurrentBatch().get(0);
    TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
    // Offset must be moved to a next message in order to proceed
    OffsetAndMetadata newOffset = new OffsetAndMetadata(record.offset() + 1);

    Mockito.doThrow(new KafkaException("Stub exception")).when(nativeConsumer).commitSync(Map.of(topicPartition, newOffset));
    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Test
    Assertions.assertThrows(KafkaException.class, () -> ack.nAcknowledge(record));

    // Shared state - offsets
    Assertions.assertTrue(consumer.getConsumerContext().getGlobalSeekedOffset(topicPartition).isEmpty());
    Assertions.assertEquals(0, consumer.getConsumerContext().getBatchSeekedOffsets().size());
    Assertions.assertNull(consumer.getConsumerContext().getBatchSeekedOffsets().get(topicPartition));
  }

  /**
   * Verifies specific message acknowledged, offset moved further and retries completed
   */

  @Test
  void nAcknowledgeSingleMessageRetry() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    // at least 3 are always created by a factory method
    ConsumerRecord<String, String> record = consumer.getConsumerContext().getCurrentBatch().get(0);
    TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
    // Offset must be moved to a next message in order to proceed
    OffsetAndMetadata newOffset = new OffsetAndMetadata(record.offset() + 1);

    // always completes retry operation
    Mockito
        .when(
            consumer
                .getRetryQueue()
                .retry(Mockito.any(ConsumerRecord.class), Mockito.any(Exception.class))
        )
        .thenReturn(CompletableFuture.completedFuture("Done"));
    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Business case when we were supposed to retry
    ack.retry(record, new UnknownLeaderEpochException("Stub exception"));

    // Test
    ack.nAcknowledge(record);

    // Verify retry awaited and passed
    Assertions.assertEquals(List.of(record).size(), consumer.getConsumerContext().getBatchRetryMessages().size());
    Assertions.assertTrue(consumer.getConsumerContext().getAllBatchRetryFuturesAsOne().isDone());

    Mockito.verify(nativeConsumer, Mockito.times(1)).commitSync(Map.of(topicPartition, newOffset));

    // Shared state - offsets
    Assertions.assertEquals(newOffset, consumer.getConsumerContext().getGlobalSeekedOffset(topicPartition).orElseThrow());
    Assertions.assertEquals(List.of(record).size(), consumer.getConsumerContext().getBatchSeekedOffsets().size());
    Assertions.assertEquals(newOffset, consumer.getConsumerContext().getBatchSeekedOffsets().get(topicPartition));
  }

  /**
   * Verifies several specific messages acknowledged and offset moved further
   */

  @Test
  void acknowledgeSeveralMessages() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    // at least 3 are always created by a factory method
    List<ConsumerRecord<String, String>> batch = consumer.getConsumerContext().getCurrentBatch();
    ConsumerRecord<String, String> firstRecord = batch.get(0);
    ConsumerRecord<String, String> secondRecord = batch.get(1);
    ConsumerRecord<String, String> thirdRecord = batch.get(2);
    List<ConsumerRecord<String, String>> recordsToAcknowledge = List.of(firstRecord, secondRecord, thirdRecord);

    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Test
    ack.acknowledge(recordsToAcknowledge);

    HashMap<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
    for (ConsumerRecord<String, String> record : recordsToAcknowledge) {
      TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
      // Offset must be moved to a next message in order to proceed
      OffsetAndMetadata newOffset = new OffsetAndMetadata(record.offset() + 1);

      offsets.put(topicPartition, newOffset);

      // Shared state - offsets
      Assertions.assertEquals(newOffset, consumer.getConsumerContext().getGlobalSeekedOffset(topicPartition).orElseThrow());
      Assertions.assertEquals(newOffset, consumer.getConsumerContext().getBatchSeekedOffsets().get(topicPartition));
    }
    // Shared state - offsets
    Assertions.assertEquals(recordsToAcknowledge.size(), consumer.getConsumerContext().getBatchSeekedOffsets().size());

    Mockito.verify(nativeConsumer, Mockito.times(1)).commitSync(offsets);
  }

  /**
   * Verifies several specific messages are NOT acknowledged and offset NOT moved further
   */

  @Test
  void acknowledgeSeveralMessagesException() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    // at least 3 are always created by a factory method
    List<ConsumerRecord<String, String>> batch = consumer.getConsumerContext().getCurrentBatch();
    ConsumerRecord<String, String> firstRecord = batch.get(0);
    ConsumerRecord<String, String> secondRecord = batch.get(1);
    ConsumerRecord<String, String> thirdRecord = batch.get(2);
    List<ConsumerRecord<String, String>> recordsToAcknowledge = List.of(firstRecord, secondRecord, thirdRecord);

    Mockito.doThrow(new KafkaException("Stub exception")).when(nativeConsumer).commitSync(Mockito.any(Map.class));
    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Test
    Assertions.assertThrows(KafkaException.class, () -> ack.acknowledge(recordsToAcknowledge));

    HashMap<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
    for (ConsumerRecord<String, String> record : recordsToAcknowledge) {
      TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
      // Offset must be moved to a next message in order to proceed
      OffsetAndMetadata newOffset = new OffsetAndMetadata(record.offset() + 1);

      offsets.put(topicPartition, newOffset);

      // Shared state - offsets
      Assertions.assertTrue(consumer.getConsumerContext().getGlobalSeekedOffset(topicPartition).isEmpty());
      Assertions.assertNull(consumer.getConsumerContext().getBatchSeekedOffsets().get(topicPartition));
    }
    // Shared state - offsets
    Assertions.assertEquals(0, consumer.getConsumerContext().getBatchSeekedOffsets().size());
  }

  /**
   * Verifies several specific messages acknowledged, offset moved further and retries completed
   */

  @Test
  void acknowledgeSeveralMessagesRetry() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    // at least 3 are always created by a factory method
    List<ConsumerRecord<String, String>> batch = consumer.getConsumerContext().getCurrentBatch();
    ConsumerRecord<String, String> retryRecord = batch.get(0);
    ConsumerRecord<String, String> secondRecord = batch.get(1);
    ConsumerRecord<String, String> thirdRecord = batch.get(2);
    List<ConsumerRecord<String, String>> recordsToAcknowledge = List.of(retryRecord, secondRecord, thirdRecord);

    // always completes retry operation
    Mockito
        .when(
            consumer
                .getRetryQueue()
                .retry(Mockito.any(ConsumerRecord.class), Mockito.any(Exception.class))
        )
        .thenReturn(CompletableFuture.completedFuture("Done"));
    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Business case when we supposed to retry
    ack.retry(retryRecord, new UnknownLeaderEpochException("Stub exception"));

    // Test
    ack.acknowledge(recordsToAcknowledge);

    // Verify retry awaited and passed
    Assertions.assertEquals(List.of(retryRecord).size(), consumer.getConsumerContext().getBatchRetryMessages().size());
    Assertions.assertTrue(consumer.getConsumerContext().getAllBatchRetryFuturesAsOne().isDone());

    HashMap<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
    for (ConsumerRecord<String, String> record : recordsToAcknowledge) {
      TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
      // Offset must be moved to a next message in order to proceed
      OffsetAndMetadata newOffset = new OffsetAndMetadata(record.offset() + 1);

      offsets.put(topicPartition, newOffset);

      // Shared state - offsets
      Assertions.assertEquals(newOffset, consumer.getConsumerContext().getGlobalSeekedOffset(topicPartition).orElseThrow());
      Assertions.assertEquals(newOffset, consumer.getConsumerContext().getBatchSeekedOffsets().get(topicPartition));
    }
    // Shared state - offsets
    Assertions.assertEquals(recordsToAcknowledge.size(), consumer.getConsumerContext().getBatchSeekedOffsets().size());

    Mockito.verify(nativeConsumer, Mockito.times(1)).commitSync(offsets);
  }

  /**
   * Verifies several specific messages acknowledged and offset moved further
   */

  @Test
  void nAcknowledgeSeveralMessages() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    // at least 3 are always created by a factory method
    List<ConsumerRecord<String, String>> batch = consumer.getConsumerContext().getCurrentBatch();
    ConsumerRecord<String, String> firstRecord = batch.get(0);
    ConsumerRecord<String, String> secondRecord = batch.get(1);
    ConsumerRecord<String, String> thirdRecord = batch.get(2);
    List<ConsumerRecord<String, String>> recordsToAcknowledge = List.of(firstRecord, secondRecord, thirdRecord);

    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Test
    ack.nAcknowledge(recordsToAcknowledge);

    HashMap<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
    for (ConsumerRecord<String, String> record : recordsToAcknowledge) {
      TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
      // Offset must be moved to a next message in order to proceed
      OffsetAndMetadata newOffset = new OffsetAndMetadata(record.offset() + 1);

      offsets.put(topicPartition, newOffset);

      // Shared state - offsets
      Assertions.assertEquals(newOffset, consumer.getConsumerContext().getGlobalSeekedOffset(topicPartition).orElseThrow());
      Assertions.assertEquals(newOffset, consumer.getConsumerContext().getBatchSeekedOffsets().get(topicPartition));
    }
    // Shared state - offsets
    Assertions.assertEquals(recordsToAcknowledge.size(), consumer.getConsumerContext().getBatchSeekedOffsets().size());

    Mockito.verify(nativeConsumer, Mockito.times(1)).commitSync(offsets);
  }

  /**
   * Verifies all messages are NOT acknowledged and offset NOT moved further if in-the-middle exception happens
   */

  @Test
  void nAcknowledgeSeveralMessagesExceptionOnSecondMessage() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    // at least 3 are always created by a factory method
    List<ConsumerRecord<String, String>> batch = consumer.getConsumerContext().getCurrentBatch();
    ConsumerRecord<String, String> firstRecord = batch.get(0);
    ConsumerRecord<String, String> secondRecord = batch.get(1);
    ConsumerRecord<String, String> thirdRecord = batch.get(2);
    List<ConsumerRecord<String, String>> recordsToAcknowledge = List.of(firstRecord, secondRecord, thirdRecord);

    DeadLetterQueue<String> dlqMock = consumer.getDeadLetterQueue();
    Mockito.doThrow(new KafkaException("Stub exception")).when(dlqMock).send(secondRecord);
    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Test
    Assertions.assertThrows(KafkaException.class, () -> ack.nAcknowledge(recordsToAcknowledge));

    HashMap<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
    for (ConsumerRecord<String, String> record : recordsToAcknowledge) {
      TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
      // Offset must be moved to a next message in order to proceed
      OffsetAndMetadata newOffset = new OffsetAndMetadata(record.offset() + 1);

      offsets.put(topicPartition, newOffset);

      // Shared state - offsets
      Assertions.assertTrue(consumer.getConsumerContext().getGlobalSeekedOffset(topicPartition).isEmpty());
      Assertions.assertNull(consumer.getConsumerContext().getBatchSeekedOffsets().get(topicPartition));

    }
    // Shared state - offsets
    Assertions.assertEquals(0, consumer.getConsumerContext().getBatchSeekedOffsets().size());
  }

  /**
   * Verifies several specific messages acknowledged, offset moved further and retries completed
   */

  @Test
  void nAcknowledgeSeveralMessagesRetry() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    // at least 3 are always created by a factory method
    List<ConsumerRecord<String, String>> batch = consumer.getConsumerContext().getCurrentBatch();
    ConsumerRecord<String, String> retryRecord = batch.get(0);
    ConsumerRecord<String, String> secondRecord = batch.get(1);
    ConsumerRecord<String, String> thirdRecord = batch.get(2);
    List<ConsumerRecord<String, String>> recordsToAcknowledge = List.of(retryRecord, secondRecord, thirdRecord);

    // always completes retry operation
    Mockito
        .when(
            consumer
                .getRetryQueue()
                .retry(Mockito.any(ConsumerRecord.class), Mockito.any(Exception.class))
        )
        .thenReturn(CompletableFuture.completedFuture("Done"));
    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Business case when we supposed to retry
    ack.retry(retryRecord, new UnknownLeaderEpochException("Stub exception"));

    // Test
    ack.nAcknowledge(recordsToAcknowledge);

    // Verify retry awaited and passed
    Assertions.assertEquals(List.of(retryRecord).size(), consumer.getConsumerContext().getBatchRetryMessages().size());
    Assertions.assertTrue(consumer.getConsumerContext().getAllBatchRetryFuturesAsOne().isDone());

    HashMap<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
    for (ConsumerRecord<String, String> record : recordsToAcknowledge) {
      TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
      // Offset must be moved to a next message in order to proceed
      OffsetAndMetadata newOffset = new OffsetAndMetadata(record.offset() + 1);

      offsets.put(topicPartition, newOffset);

      // Shared state - offsets
      Assertions.assertEquals(newOffset, consumer.getConsumerContext().getGlobalSeekedOffset(topicPartition).orElseThrow());
      Assertions.assertEquals(newOffset, consumer.getConsumerContext().getBatchSeekedOffsets().get(topicPartition));
    }
    // Shared state - offsets
    Assertions.assertEquals(recordsToAcknowledge.size(), consumer.getConsumerContext().getBatchSeekedOffsets().size());

    Mockito.verify(nativeConsumer, Mockito.times(1)).commitSync(offsets);
  }

  /**
   * Verifies several specific offsets are commited
   */

  @Test
  void commit() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    // at least 3 are always created by a factory method
    List<ConsumerRecord<String, String>> batch = consumer.getConsumerContext().getCurrentBatch();
    ConsumerRecord<String, String> firstRecord = batch.get(0);
    ConsumerRecord<String, String> secondRecord = batch.get(1);
    ConsumerRecord<String, String> thirdRecord = batch.get(2);
    List<ConsumerRecord<String, String>> recordsToCommit = List.of(firstRecord, secondRecord, thirdRecord);

    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Test
    ack.commit(recordsToCommit);

    Map<TopicPartition, OffsetAndMetadata> offsets = recordsToCommit.stream().collect(
        Collectors.toMap(
            (r) -> new TopicPartition(r.topic(), r.partition()),
            // Offset must be moved to a next message in order to proceed
            (r) -> new OffsetAndMetadata(r.offset() + 1)
        )
    );
    Mockito.verify(nativeConsumer, Mockito.times(1)).commitSync(offsets);
  }

  /**
   * Verifies several specific offsets are NOT commited
   */

  @Test
  void commitException() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    // at least 3 are always created by a factory method
    List<ConsumerRecord<String, String>> batch = consumer.getConsumerContext().getCurrentBatch();
    ConsumerRecord<String, String> firstRecord = batch.get(0);
    ConsumerRecord<String, String> secondRecord = batch.get(1);
    ConsumerRecord<String, String> thirdRecord = batch.get(2);
    List<ConsumerRecord<String, String>> recordsToCommit = List.of(firstRecord, secondRecord, thirdRecord);

    Mockito.doThrow(new KafkaException("Stub exception")).when(nativeConsumer).commitSync(Mockito.any(Map.class));
    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Test
    Assertions.assertThrows(KafkaException.class, () -> ack.commit(recordsToCommit));
  }

  /**
   * Verifies several specific offsets are commited and retries completed
   */

  @Test
  void commitRetry() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    // at least 3 are always created by a factory method
    List<ConsumerRecord<String, String>> batch = consumer.getConsumerContext().getCurrentBatch();
    ConsumerRecord<String, String> retryRecord = batch.get(0);
    ConsumerRecord<String, String> secondRecord = batch.get(1);
    ConsumerRecord<String, String> thirdRecord = batch.get(2);
    List<ConsumerRecord<String, String>> recordsToCommit = List.of(retryRecord, secondRecord, thirdRecord);

    // always completes retry operation
    Mockito
        .when(
            consumer
                .getRetryQueue()
                .retry(Mockito.any(ConsumerRecord.class), Mockito.any(Exception.class))
        )
        .thenReturn(CompletableFuture.completedFuture("Done"));
    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Business case when we supposed to retry
    ack.retry(retryRecord, new UnknownLeaderEpochException("Stub exception"));

    // Test
    ack.commit(recordsToCommit);

    // Verify retry awaited and passed
    Assertions.assertEquals(List.of(retryRecord).size(), consumer.getConsumerContext().getBatchRetryMessages().size());
    Assertions.assertTrue(consumer.getConsumerContext().getAllBatchRetryFuturesAsOne().isDone());

    Map<TopicPartition, OffsetAndMetadata> offsets = recordsToCommit.stream().collect(
        Collectors.toMap(
            (r) -> new TopicPartition(r.topic(), r.partition()),
            // Offset must be moved to a next message in order to proceed
            (r) -> new OffsetAndMetadata(r.offset() + 1)
        )
    );
    Mockito.verify(nativeConsumer, Mockito.times(1)).commitSync(offsets);
  }

  /**
   * Verifies offset moved to a specific point and are NOT commited
   */

  @Test
  void seek() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    // at least 3 are always created by a factory method
    ConsumerRecord<String, String> record = consumer.getConsumerContext().getCurrentBatch().get(0);
    TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
    // Offset must be moved to a next message in order to proceed
    OffsetAndMetadata newOffset = new OffsetAndMetadata(record.offset() + 1);

    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Test
    ack.seek(record);

    // This operation MUST NOT do any commits
    Mockito.verify(nativeConsumer, Mockito.never()).commitSync();
    Mockito.verify(nativeConsumer, Mockito.never()).commitSync(Mockito.any(Duration.class));
    Mockito.verify(nativeConsumer, Mockito.never()).commitSync(Mockito.any(Map.class));
    Mockito.verify(nativeConsumer, Mockito.never()).commitSync(Mockito.any(Map.class), Mockito.any(Duration.class));
    Mockito.verify(nativeConsumer, Mockito.never()).commitAsync();
    Mockito.verify(nativeConsumer, Mockito.never()).commitAsync(Mockito.any(OffsetCommitCallback.class));
    Mockito.verify(nativeConsumer, Mockito.never()).commitAsync(Mockito.any(Map.class), Mockito.any(OffsetCommitCallback.class));

    // Shared state - offsets
    Assertions.assertEquals(newOffset, consumer.getConsumerContext().getGlobalSeekedOffset(topicPartition).orElseThrow());
    Assertions.assertEquals(List.of(record).size(), consumer.getConsumerContext().getBatchSeekedOffsets().size());
    Assertions.assertEquals(newOffset, consumer.getConsumerContext().getBatchSeekedOffsets().get(topicPartition));
  }

  /**
   * Verifies offset moved to a specific point, are NOT commited and retries completed
   */

  @Test
  void seekRetry() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    // at least 3 are always created by a factory method
    ConsumerRecord<String, String> record = consumer.getConsumerContext().getCurrentBatch().get(0);
    TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
    // Offset must be moved to a next message in order to proceed
    OffsetAndMetadata newOffset = new OffsetAndMetadata(record.offset() + 1);

    // always completes retry operation
    Mockito
        .when(
            consumer
                .getRetryQueue()
                .retry(Mockito.any(ConsumerRecord.class), Mockito.any(Exception.class))
        )
        .thenReturn(CompletableFuture.completedFuture("Done"));
    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Business case when we supposed to retry
    ack.retry(record, new UnknownLeaderEpochException("Stub exception"));

    // Test
    ack.seek(record);

    // Verify retry awaited and passed
    Assertions.assertEquals(List.of(record).size(), consumer.getConsumerContext().getBatchRetryMessages().size());
    Assertions.assertTrue(consumer.getConsumerContext().getAllBatchRetryFuturesAsOne().isDone());

    // This operation MUST NOT do any commits
    Mockito.verify(nativeConsumer, Mockito.never()).commitSync();
    Mockito.verify(nativeConsumer, Mockito.never()).commitSync(Mockito.any(Duration.class));
    Mockito.verify(nativeConsumer, Mockito.never()).commitSync(Mockito.any(Map.class));
    Mockito.verify(nativeConsumer, Mockito.never()).commitSync(Mockito.any(Map.class), Mockito.any(Duration.class));
    Mockito.verify(nativeConsumer, Mockito.never()).commitAsync();
    Mockito.verify(nativeConsumer, Mockito.never()).commitAsync(Mockito.any(OffsetCommitCallback.class));
    Mockito.verify(nativeConsumer, Mockito.never()).commitAsync(Mockito.any(Map.class), Mockito.any(OffsetCommitCallback.class));

    // Shared state - offsets
    Assertions.assertEquals(newOffset, consumer.getConsumerContext().getGlobalSeekedOffset(topicPartition).orElseThrow());
    Assertions.assertEquals(List.of(record).size(), consumer.getConsumerContext().getBatchSeekedOffsets().size());
    Assertions.assertEquals(newOffset, consumer.getConsumerContext().getBatchSeekedOffsets().get(topicPartition));
  }

  /**
   * Verifies retries scheduled and completed
   */

  @Test
  void retry() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    // at least 3 are always created by a factory method
    ConsumerRecord<String, String> record = consumer.getConsumerContext().getCurrentBatch().get(0);
    TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
    // Offset must be moved to a next message in order to proceed
    OffsetAndMetadata newOffset = new OffsetAndMetadata(record.offset() + 1);

    // always completes retry operation
    Mockito
        .when(
            consumer
                .getRetryQueue()
                .retry(Mockito.any(ConsumerRecord.class), Mockito.any(Exception.class)))
        .thenReturn(CompletableFuture.completedFuture("Done")
        );
    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Test
    ack.retry(record, new UnknownLeaderEpochException("Stub exception"));

    // Verify retry scheduled
    Assertions.assertEquals(List.of(record).size(), consumer.getConsumerContext().getBatchRetryMessages().size());
    Assertions.assertTrue(consumer.getConsumerContext().getAllBatchRetryFuturesAsOne().isDone());

    // Perform operations and await retry
    ack.acknowledge();

    // Verify we seeked to next offset after retry
    Assertions.assertEquals(newOffset, consumer.getConsumerContext().getGlobalSeekedOffset(topicPartition).orElseThrow());
    Assertions.assertEquals(List.of(record).size(), consumer.getConsumerContext().getBatchSeekedOffsets().size());
    Assertions.assertEquals(newOffset, consumer.getConsumerContext().getBatchSeekedOffsets().get(topicPartition));
  }

  private KafkaInternalTopicAck<String> createAck(
      KafkaConsumer<String> consumer,
      org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer
  ) {
    return new KafkaInternalTopicAck<>(consumer, nativeConsumer);
  }

  private KafkaConsumer<String> createMock() {
    KafkaConsumer<String> consumer = Mockito.mock(KafkaConsumer.class);

    ConsumerContext<String> context = new ConsumerContext<>();
    List<ConsumerRecord<String, String>> records = new ArrayList<>();
    for (int i = 0; i < RANDOM.nextInt(3, 5); i++) {
      records.add(createRecord());
    }
    context.prepareForNextBatch(records);
    Mockito.when(consumer.getConsumerContext()).thenReturn(context);

    DeadLetterQueue<String> dlq = Mockito.mock(DeadLetterQueue.class);
    Mockito.when(consumer.getDeadLetterQueue()).thenReturn(dlq);

    RetryQueue<String> retryQueue = Mockito.mock(RetryQueue.class);
    Mockito.when(consumer.getRetryQueue()).thenReturn(retryQueue);
    return consumer;
  }

  private org.apache.kafka.clients.consumer.KafkaConsumer<String, String> createNativeMock() {
    return Mockito.mock(org.apache.kafka.clients.consumer.KafkaConsumer.class);
  }

  private ConsumerRecord<String, String> createRecord() {
    return new ConsumerRecord<>(
        UUID.randomUUID().toString(),
        RANDOM.nextInt(1, 5),
        RANDOM.nextLong(1, 5),
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString()
    );
  }
}
