package ru.hh.nab.kafka.consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class InMemorySeekOnlyAckTest {
  private static final Random RANDOM = new Random();

  /**
   * Verifies all messages are sent into DLQ
   */

  @Test
  void nAcknowledgeAllMessages() {
    KafkaConsumer<String> consumer = createMock();
    int batchSize = consumer.getConsumerContext().getCurrentBatch().size();

    // Test
    InMemorySeekOnlyAck<String> ack = createAck(consumer);
    ack.nAcknowledge();

    // All messages must be sent to DLQ
    Mockito.verify(consumer.getDeadLetterQueue(), Mockito.times(batchSize)).send(Mockito.any(ConsumerRecord.class));
  }

  /**
   * Verifies only single specific message is sent into DLQ
   */

  @Test
  void nAcknowledgeSingleMessage() {
    KafkaConsumer<String> consumer = createMock();
    // at least three always created by a factory method
    ConsumerRecord<String, String> singleMessage = consumer.getConsumerContext().getCurrentBatch().get(0);

    // Test
    InMemorySeekOnlyAck<String> ack = createAck(consumer);
    ack.nAcknowledge(singleMessage);

    Mockito.verify(consumer.getDeadLetterQueue(), Mockito.times(1)).send(singleMessage);
    // Other messages must not be sent to DLQ
    Mockito.verify(consumer.getDeadLetterQueue(), Mockito.times(1)).send(Mockito.any(ConsumerRecord.class));
  }

  /**
   * Verifies only several specific messages are sent into DLQ
   */

  @Test
  void nAcknowledgeSeveralMessages() {
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
    ack.nAcknowledge(List.of(firstMessage, secondMessage, thirdMessage));

    Mockito.verify(consumer.getDeadLetterQueue(), Mockito.times(1)).send(firstMessage);
    Mockito.verify(consumer.getDeadLetterQueue(), Mockito.times(1)).send(secondMessage);
    Mockito.verify(consumer.getDeadLetterQueue(), Mockito.times(1)).send(thirdMessage);
    // Other messages must not be sent to DLQ
    Mockito.verify(consumer.getDeadLetterQueue(), Mockito.times(3)).send(Mockito.any(ConsumerRecord.class));
  }

  /**
   * Verifies retries are not allowed
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
    Mockito.when(consumer.getConsumerContext()).thenReturn(context);

    DeadLetterQueue<String> dlq = Mockito.mock(DeadLetterQueue.class);
    Mockito.when(consumer.getDeadLetterQueue()).thenReturn(dlq);
    return consumer;
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
