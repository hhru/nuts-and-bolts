package ru.hh.nab.kafka.producer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

/**
 * Unit tests for {@link DefaultKafkaProducer} class.
 * These tests verify the behavior of message sending functionality with different types of messages
 * and ensure proper conversion between Spring's Kafka types and our custom types.
 */
class DefaultKafkaProducerTest {

  private final Executor executor = Runnable::run;
  private KafkaTemplate<String, Object> kafkaTemplate;
  private DefaultKafkaProducer producer;

  @BeforeEach
  void setUp() {
    kafkaTemplate = mock(KafkaTemplate.class);
    producer = new DefaultKafkaProducer(kafkaTemplate);
  }

  /**
   * Tests sending a message with a primitive value.
   * Verifies that:
   * <ul>
   *     <li>The message is sent correctly through KafkaTemplate</li>
   *     <li>The topic, key, and value are preserved in the result</li>
   *     <li>The metadata is correctly converted from Spring's SendResult to our KafkaSendResult</li>
   *     <li>The KafkaTemplate's send method is called with the correct parameters</li>
   * </ul>
   */
  @Test
  void testSendMessageWithPrimitiveValue() {
    // given
    String topic = "test-topic";
    String key = "test-key";
    Integer value = 1;
    ProducerRecord<String, Integer> record = new ProducerRecord<>(topic, key, value);

    RecordMetadata metadata = new RecordMetadata(
        new TopicPartition(topic, 0), 0, 0, 0, 0, 0
    );
    SendResult<String, Object> sendResult = new SendResult<>(
        new ProducerRecord<>(topic, key, value),
        metadata
    );

    when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(sendResult));

    // when
    CompletableFuture<KafkaSendResult<Integer>> future = producer.sendMessage(record, executor);

    // then
    KafkaSendResult<Integer> result = future.join();
    assertEquals(topic, result.getProducerRecord().topic());
    assertEquals(key, result.getProducerRecord().key());
    assertEquals(value, result.getProducerRecord().value());
    assertEquals(metadata, result.getRecordMetadata());
    verify(kafkaTemplate).send(any(ProducerRecord.class));
  }

  /**
   * Tests sending a message with a null value.
   * Verifies that:
   * <ul>
   *     <li>The message is sent correctly through KafkaTemplate</li>
   *     <li>The null value is preserved in the result</li>
   *     <li>The metadata is correctly converted from Spring's SendResult to our KafkaSendResult</li>
   *     <li>The KafkaTemplate's send method is called with the correct parameters</li>
   * </ul>
   */
  @Test
  void testSendMessageWithNullValue() {
    // given
    String topic = "test-topic";
    String key = "test-key";
    ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, null);

    RecordMetadata metadata = new RecordMetadata(
        new TopicPartition(topic, 0), 0, 0, 0, 0, 0
    );
    SendResult<String, Object> sendResult = new SendResult<>(
        new ProducerRecord<>(topic, key, null),
        metadata
    );

    when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(sendResult));

    // when
    CompletableFuture<KafkaSendResult<String>> future = producer.sendMessage(record, executor);

    // then
    KafkaSendResult<String> result = future.join();
    assertEquals(topic, result.getProducerRecord().topic());
    assertEquals(key, result.getProducerRecord().key());
    assertNull(result.getProducerRecord().value());
    assertEquals(metadata, result.getRecordMetadata());
    verify(kafkaTemplate).send(any(ProducerRecord.class));
  }

  /**
   * Tests sending a message with a custom object type.
   * Verifies that:
   * <ul>
   *     <li>The message is sent correctly through KafkaTemplate</li>
   *     <li>The custom object is preserved in the result</li>
   *     <li>The metadata is correctly converted from Spring's SendResult to our KafkaSendResult</li>
   *     <li>The KafkaTemplate's send method is called with the correct parameters</li>
   * </ul>
   */
  @Test
  void testSendMessageWithCustomObjectType() {
    // given
    record TestMessage(String data) {
    }

    String topic = "test-topic";
    String key = "test-key";
    TestMessage value = new TestMessage("test-data");
    ProducerRecord<String, TestMessage> record = new ProducerRecord<>(topic, key, value);

    RecordMetadata metadata = new RecordMetadata(
        new TopicPartition(topic, 0), 0, 0, 0, 0, 0
    );
    SendResult<String, Object> sendResult = new SendResult<>(
        new ProducerRecord<>(topic, key, value),
        metadata
    );

    when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(sendResult));

    // when
    CompletableFuture<KafkaSendResult<TestMessage>> future = producer.sendMessage(record, executor);

    // then
    KafkaSendResult<TestMessage> result = future.join();
    assertEquals(topic, result.getProducerRecord().topic());
    assertEquals(key, result.getProducerRecord().key());
    assertEquals(value, result.getProducerRecord().value());
    assertEquals(metadata, result.getRecordMetadata());
    verify(kafkaTemplate).send(any(ProducerRecord.class));
  }
}
