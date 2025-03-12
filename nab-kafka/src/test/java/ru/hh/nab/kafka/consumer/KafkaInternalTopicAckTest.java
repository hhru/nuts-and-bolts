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
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.UnknownLeaderEpochException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.hh.nab.kafka.exception.ConfigurationException;
import ru.hh.nab.kafka.producer.KafkaSendResult;

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
    consumer.getConsumerContext().addFutureMessage(CompletableFuture.completedFuture(null), record);

    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Test
    ack.acknowledge();

    Assertions.assertTrue(consumer.getConsumerContext().getAllBatchFuturesAsOne().isDone());
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
    Assertions.assertEquals(List.of(record).size(), consumer.getConsumerContext().getBatchFutureMessages().size());
    Assertions.assertTrue(consumer.getConsumerContext().getAllBatchFuturesAsOne().isDone());

    Mockito.verify(nativeConsumer, Mockito.times(1)).commitSync(Map.of(topicPartition, newOffset));

    // Shared state - offsets
    Assertions.assertEquals(newOffset, consumer.getConsumerContext().getGlobalSeekedOffset(topicPartition).orElseThrow());
    Assertions.assertEquals(List.of(record).size(), consumer.getConsumerContext().getBatchSeekedOffsets().size());
    Assertions.assertEquals(newOffset, consumer.getConsumerContext().getBatchSeekedOffsets().get(topicPartition));
  }

  /**
   * Verifies specific message acknowledged
   */

  @Test
  void sendToDlqSingleMessage() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    // at least 3 are always created by a factory method
    ConsumerRecord<String, String> record = consumer.getConsumerContext().getCurrentBatch().get(0);

    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Test
    ack.sendToDlq(record);

    // Verify sendToDlq awaited and passed
    Assertions.assertTrue(consumer.getConsumerContext().getBatchFutureMessages().contains(record));
    Assertions.assertEquals(List.of(record).size(), consumer.getConsumerContext().getBatchFutures().size());
    Assertions.assertTrue(consumer.getConsumerContext().getAllBatchFuturesAsOne().isDone());
  }

  /**
   * Verifies that user can't perform sendToDlq for a single message if DLQ is not configured
   */

  @Test
  void sendToDlqSingleMessageInvalidConfiguration() {
    KafkaConsumer<String> consumer = createMock();
    Mockito.when(consumer.getDeadLetterQueue()).thenReturn(null);
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    // at least 3 are always created by a factory method
    ConsumerRecord<String, String> record = consumer.getConsumerContext().getCurrentBatch().get(0);
    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Test
    Assertions.assertThrows(ConfigurationException.class, () -> ack.sendToDlq(record));

    Assertions.assertEquals(0, consumer.getConsumerContext().getBatchFutureMessages().size());
    Assertions.assertEquals(0, consumer.getConsumerContext().getBatchFutures().size());
  }

  /**
   * Verifies specific message acknowledged and retries completed
   */

  @Test
  void sendToDlqSingleMessageRetry() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    // at least 3 are always created by a factory method
    ConsumerRecord<String, String> record = consumer.getConsumerContext().getCurrentBatch().get(0);

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
    ack.sendToDlq(record);

    // Verify retry and sentToDlq awaited and passed
    Assertions.assertTrue(consumer.getConsumerContext().getBatchFutureMessages().contains(record));
    Assertions.assertEquals(List.of(record).size() + List.of(record).size(), consumer.getConsumerContext().getBatchFutures().size());
    Assertions.assertTrue(consumer.getConsumerContext().getAllBatchFuturesAsOne().isDone());
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
   * Verifies that user can't perform sendToDlq for several messages if DLQ is not configured
   */

  @Test
  void sendToDlqSeveralMessagesInvalidConfiguration() {
    KafkaConsumer<String> consumer = createMock();
    Mockito.when(consumer.getDeadLetterQueue()).thenReturn(null);
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    // at least 3 are always created by a factory method
    List<ConsumerRecord<String, String>> batch = consumer.getConsumerContext().getCurrentBatch();
    ConsumerRecord<String, String> firstRecord = batch.get(0);
    ConsumerRecord<String, String> secondRecord = batch.get(1);
    ConsumerRecord<String, String> thirdRecord = batch.get(2);
    List<ConsumerRecord<String, String>> recordsToAcknowledge = List.of(firstRecord, secondRecord, thirdRecord);

    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Test
    Assertions.assertThrows(ConfigurationException.class, () -> ack.sendToDlq(recordsToAcknowledge));

    Assertions.assertEquals(0, consumer.getConsumerContext().getBatchFutureMessages().size());
    Assertions.assertEquals(0, consumer.getConsumerContext().getBatchFutures().size());
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
    Assertions.assertEquals(List.of(retryRecord).size(), consumer.getConsumerContext().getBatchFutureMessages().size());
    Assertions.assertTrue(consumer.getConsumerContext().getAllBatchFuturesAsOne().isDone());

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
   * Verifies several specific messages acknowledged
   */

  @Test
  void sendToDlqSeveralMessages() {
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
    ack.sendToDlq(recordsToAcknowledge);

    // Verify sendToDlq awaited and passed
    Assertions.assertTrue(consumer.getConsumerContext().getBatchFutureMessages().containsAll(recordsToAcknowledge));
    Assertions.assertEquals(recordsToAcknowledge.size(), consumer.getConsumerContext().getBatchFutures().size());
    Assertions.assertTrue(consumer.getConsumerContext().getAllBatchFuturesAsOne().isDone());
  }

  /**
   * Verifies only messages prior to exception are sent into DLQ if exception happens in-the-middle
   */

  @Test
  void sendToDlqSeveralMessagesExceptionOnSecondMessage() {
    KafkaConsumer<String> consumer = createMock();
    org.apache.kafka.clients.consumer.KafkaConsumer<String, String> nativeConsumer = createNativeMock();

    // at least 3 are always created by a factory method
    List<ConsumerRecord<String, String>> batch = consumer.getConsumerContext().getCurrentBatch();
    ConsumerRecord<String, String> firstRecord = batch.get(0);
    ConsumerRecord<String, String> failedRecord = batch.get(1);
    ConsumerRecord<String, String> thirdRecord = batch.get(2);
    List<ConsumerRecord<String, String>> recordsToAcknowledge = List.of(firstRecord, failedRecord, thirdRecord);

    DeadLetterQueue<String> dlqMock = consumer.getDeadLetterQueue();
    Mockito.doThrow(new KafkaException("Stub exception")).when(dlqMock).send(failedRecord);
    KafkaInternalTopicAck<String> ack = createAck(consumer, nativeConsumer);

    // Test
    Assertions.assertThrows(KafkaException.class, () -> ack.sendToDlq(recordsToAcknowledge));

    // Only first message must be sent to DLQ because second message will result in exception
    Assertions.assertTrue(consumer.getConsumerContext().getBatchFutureMessages().contains(firstRecord));
    Assertions.assertEquals(List.of(firstRecord).size(), consumer.getConsumerContext().getBatchFutures().size());
    Assertions.assertTrue(consumer.getConsumerContext().getAllBatchFuturesAsOne().isDone());
  }

  /**
   * Verifies several specific messages acknowledged and retries completed
   */

  @Test
  void sendToDlqSeveralMessagesRetry() {
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
    ack.sendToDlq(recordsToAcknowledge);

    // Verify retry and sendToDlq awaited and passed
    Assertions.assertTrue(consumer.getConsumerContext().getBatchFutureMessages().containsAll(recordsToAcknowledge));
    Assertions.assertEquals(List.of(retryRecord).size() + recordsToAcknowledge.size(), consumer.getConsumerContext().getBatchFutures().size());
    Assertions.assertTrue(consumer.getConsumerContext().getAllBatchFuturesAsOne().isDone());
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
    Assertions.assertEquals(List.of(retryRecord).size(), consumer.getConsumerContext().getBatchFutureMessages().size());
    Assertions.assertTrue(consumer.getConsumerContext().getAllBatchFuturesAsOne().isDone());

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
    Assertions.assertEquals(List.of(record).size(), consumer.getConsumerContext().getBatchFutureMessages().size());
    Assertions.assertTrue(consumer.getConsumerContext().getAllBatchFuturesAsOne().isDone());

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
    Assertions.assertEquals(List.of(record).size(), consumer.getConsumerContext().getBatchFutureMessages().size());
    Assertions.assertTrue(consumer.getConsumerContext().getAllBatchFuturesAsOne().isDone());

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
    Mockito.when(dlq.send(Mockito.any(ConsumerRecord.class))).thenReturn(CompletableFuture.completedFuture(createSendResult()));
    Mockito.when(consumer.getDeadLetterQueue()).thenReturn(dlq);

    RetryQueue<String> retryQueue = Mockito.mock(RetryQueue.class);
    Mockito.when(consumer.getRetryQueue()).thenReturn(retryQueue);
    return consumer;
  }

  private KafkaSendResult<String> createSendResult() {
    String topicName = UUID.randomUUID().toString();
    String value = null;
    ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topicName, value);
    RecordMetadata recordMetadata = new RecordMetadata(new TopicPartition(topicName, 1), 0, 1, System.currentTimeMillis(), 0, -1);
    return new KafkaSendResult<>(producerRecord, recordMetadata);
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
