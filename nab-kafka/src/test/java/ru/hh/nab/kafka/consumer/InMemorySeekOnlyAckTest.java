package ru.hh.nab.kafka.consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.hh.nab.kafka.exception.ConfigurationException;
import ru.hh.nab.kafka.producer.KafkaSendResult;

class InMemorySeekOnlyAckTest {
  private static final Random RANDOM = new Random();

  /**
   * Verifies only single specific message is sent into DLQ
   */

  @Test
  void sendToDlqSingleMessage() {
    KafkaConsumer<String> consumer = createMock();
    // at least three always created by a factory method
    ConsumerRecord<String, String> singleMessage = consumer.getConsumerContext().getCurrentBatch().get(0);

    // Test
    InMemorySeekOnlyAck<String> ack = createAck(consumer);
    ack.sendToDlq(singleMessage);

    Mockito.verify(consumer.getDeadLetterQueue(), Mockito.times(1)).send(singleMessage);
    // Other messages must not be sent to DLQ
    Mockito.verify(consumer.getDeadLetterQueue(), Mockito.times(1)).send(Mockito.any(ConsumerRecord.class));
  }

  /**
   * Verifies only several specific messages are sent into DLQ
   */

  @Test
  void sendToDlqSeveralMessages() {
    KafkaConsumer<String> consumer = createMock();
    // at least three always created by a factory method
    List<ConsumerRecord<String, String>> batch = consumer.getConsumerContext().getCurrentBatch();
    ConsumerRecord<String, String> firstMessage = batch.get(0);
    ConsumerRecord<String, String> secondMessage = batch.get(1);
    ConsumerRecord<String, String> thirdMessage = batch.get(2);
    // add one extra record to ensure that we always have more than 3 messages to check later
    batch.add(createRecord());

    // Test
    InMemorySeekOnlyAck<String> ack = createAck(consumer);
    ack.sendToDlq(List.of(firstMessage, secondMessage, thirdMessage));

    Mockito.verify(consumer.getDeadLetterQueue(), Mockito.times(1)).send(firstMessage);
    Mockito.verify(consumer.getDeadLetterQueue(), Mockito.times(1)).send(secondMessage);
    Mockito.verify(consumer.getDeadLetterQueue(), Mockito.times(1)).send(thirdMessage);
    // Other messages must not be sent to DLQ
    Mockito.verify(consumer.getDeadLetterQueue(), Mockito.times(3)).send(Mockito.any(ConsumerRecord.class));
  }

  /**
   * Verifies seek performed only after message is sent into DLQ
   */

  @Test
  void sendToDlqSingleMessageFailed() {
    KafkaConsumer<String> consumer = createMock();
    // at least three always created by a factory method
    ConsumerRecord<String, String> singleMessage = consumer.getConsumerContext().getCurrentBatch().get(0);

    ConsumerContext<String> consumerContext = Mockito.mock(ConsumerContext.class);
    Mockito.when(consumer.getConsumerContext()).thenReturn(consumerContext);
    Mockito.when(consumerContext.getCurrentBatch()).thenReturn(List.of(singleMessage));

    Mockito.when(consumer.getDeadLetterQueue().send(singleMessage)).thenThrow(new KafkaException("Stub exception"));

    // Test
    InMemorySeekOnlyAck<String> ack = createAck(consumer);
    Assertions.assertThrows(KafkaException.class, () -> ack.sendToDlq(singleMessage));

    Assertions.assertEquals(0, consumerContext.getBatchFutures().size());
    Assertions.assertNull(consumerContext.getAllBatchFuturesAsOne());
  }

  /**
   * Verifies that user can't perform sendToDlq for a single message if DLQ is not configured
   */

  @Test
  void sendToDlqSingleMessageInvalidConfiguration() {
    KafkaConsumer<String> consumer = createMock();
    Mockito.when(consumer.getDeadLetterQueue()).thenReturn(null);
    // at least three always created by a factory method
    ConsumerRecord<String, String> singleMessage = consumer.getConsumerContext().getCurrentBatch().get(0);

    ConsumerContext<String> consumerContext = Mockito.mock(ConsumerContext.class);
    Mockito.when(consumer.getConsumerContext()).thenReturn(consumerContext);
    Mockito.when(consumerContext.getCurrentBatch()).thenReturn(List.of(singleMessage));

    // Test
    InMemorySeekOnlyAck<String> ack = createAck(consumer);
    Assertions.assertThrows(ConfigurationException.class, () -> ack.sendToDlq(singleMessage));

    Assertions.assertEquals(0, consumerContext.getBatchFutures().size());
    Assertions.assertNull(consumerContext.getAllBatchFuturesAsOne());
  }

  /**
   * Verifies seek performed only after messages are sent into DLQ
   */

  @Test
  void sendToDlqSeveralMessageFailed() {
    KafkaConsumer<String> consumer = createMock();
    // at least three always created by a factory method
    ConsumerRecord<String, String> firstMessage = consumer.getConsumerContext().getCurrentBatch().get(0);
    ConsumerRecord<String, String> secondMessage = consumer.getConsumerContext().getCurrentBatch().get(0);
    ConsumerRecord<String, String> thirdMessage = consumer.getConsumerContext().getCurrentBatch().get(0);
    List<ConsumerRecord<String, String>> severalMessages = List.of(firstMessage, secondMessage, thirdMessage);

    ConsumerContext<String> consumerContext = Mockito.mock(ConsumerContext.class);
    Mockito.when(consumer.getConsumerContext()).thenReturn(consumerContext);
    Mockito.when(consumerContext.getCurrentBatch()).thenReturn(severalMessages);

    Mockito.when(consumer.getDeadLetterQueue().send(secondMessage)).thenThrow(new KafkaException("Stub exception"));

    // Test
    InMemorySeekOnlyAck<String> ack = createAck(consumer);
    Assertions.assertThrows(KafkaException.class, () -> ack.sendToDlq(severalMessages));

    Assertions.assertEquals(0, consumerContext.getBatchFutures().size());
    Assertions.assertNull(consumerContext.getAllBatchFuturesAsOne());
  }

  /**
   * Verifies that user can't perform sendToDlq for several messages if DLQ is not configured
   */

  @Test
  void sendToDlqSeveralMessagesInvalidConfiguration() {
    KafkaConsumer<String> consumer = createMock();
    Mockito.when(consumer.getDeadLetterQueue()).thenReturn(null);
    // at least three always created by a factory method
    ConsumerRecord<String, String> firstMessage = consumer.getConsumerContext().getCurrentBatch().get(0);
    ConsumerRecord<String, String> secondMessage = consumer.getConsumerContext().getCurrentBatch().get(0);
    ConsumerRecord<String, String> thirdMessage = consumer.getConsumerContext().getCurrentBatch().get(0);
    List<ConsumerRecord<String, String>> severalMessages = List.of(firstMessage, secondMessage, thirdMessage);

    ConsumerContext<String> consumerContext = Mockito.mock(ConsumerContext.class);
    Mockito.when(consumer.getConsumerContext()).thenReturn(consumerContext);

    // Test
    InMemorySeekOnlyAck<String> ack = createAck(consumer);
    Assertions.assertThrows(ConfigurationException.class, () -> ack.sendToDlq(severalMessages));

    Assertions.assertEquals(0, consumerContext.getBatchFutures().size());
    Assertions.assertNull(consumerContext.getAllBatchFuturesAsOne());
  }

  /**
   * Verifies sendToDlq are not allowed
   */

  @Test
  void retry() {
    KafkaConsumer<String> consumer = createMock();
    // at least three always created by a factory method
    ConsumerRecord<String, String> singleMessage = consumer.getConsumerContext().getCurrentBatch().get(0);

    // Test
    InMemorySeekOnlyAck<String> ack = createAck(consumer);
    Assertions.assertThrowsExactly(UnsupportedOperationException.class, () -> ack.retry(singleMessage, new RuntimeException("Stub exception")));

    // Messages must not be sent to DLQ
    Mockito.verify(consumer.getDeadLetterQueue(), Mockito.never()).send(Mockito.any(ConsumerRecord.class));
  }

  private InMemorySeekOnlyAck<String> createAck(KafkaConsumer<String> consumer) {
    return new InMemorySeekOnlyAck<>(consumer);
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
    return consumer;
  }

  private KafkaSendResult<String> createSendResult() {
    String topicName = UUID.randomUUID().toString();
    String value = null;
    ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topicName, value);
    RecordMetadata recordMetadata = new RecordMetadata(new TopicPartition(topicName, 1), 0, 1, System.currentTimeMillis(), 0, -1);
    return new KafkaSendResult<>(producerRecord, recordMetadata);
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
