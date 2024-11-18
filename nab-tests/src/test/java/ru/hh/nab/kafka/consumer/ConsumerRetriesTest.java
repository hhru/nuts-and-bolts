package ru.hh.nab.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hh.nab.kafka.KafkaTestConfig;
import ru.hh.nab.kafka.consumer.retry.RetryPolicyResolver;
import ru.hh.nab.kafka.consumer.retry.RetryTopics;
import ru.hh.nab.kafka.consumer.retry.policy.RetryPolicy;
import ru.hh.nab.kafka.producer.KafkaProducer;
import ru.hh.nab.kafka.producer.KafkaSendResult;

@ExtendWith(MockitoExtension.class)
public class ConsumerRetriesTest extends KafkaConsumerTestbase {
  @Mock
  Consumer<String> mockService;

  KafkaProducer retryProducer = new KafkaProducer() {
    @Override
    public <T> CompletableFuture<KafkaSendResult<T>> sendMessage(ProducerRecord<String, T> record, Executor executor) {
      return sendToKafka(record);
    }
  };

  private KafkaConsumer<String> consumer;

  @AfterEach
  void tearDown() {
    if (consumer != null) {
      consumer.stop();
    }
  }

  @Test
  void retryOnceUsingSingleTopic() throws InterruptedException {
    doThrow(new RuntimeException()).doNothing().when(mockService).accept(anyString());
    kafkaTestUtils.sendMessage(topicName, "first pancake");
    startConsumerWithRetries();
    waitUntil(() -> {
      checkPartitionsAssigned();
      verify(mockService, times(2)).accept(eq("first pancake"));
    });
    assertEquals(1, countMessagesInTopic(getDefaultRetryTopic()));
  }

  @Test
  void retry3TimesUsingSingleTopic() throws InterruptedException {
    doThrow(new RuntimeException(), new RuntimeException(), new RuntimeException()).doNothing().when(mockService).accept(anyString());
    kafkaTestUtils.sendMessage(topicName, "first pancake");
    startConsumerWithRetries();
    waitUntil(() -> {
      checkPartitionsAssigned();
      verify(mockService, times(4)).accept(eq("first pancake"));
    });
    assertEquals(3, countMessagesInTopic(getDefaultRetryTopic()));
  }

  @Test
  void retry3MessagesInARow() throws InterruptedException {
    Stream.of("first pancake", "two left feet", "three know it").forEach(message -> {
      doThrow(new RuntimeException()).doNothing().when(mockService).accept(eq(message));
      kafkaTestUtils.sendMessage(topicName, message);
    });
    doNothing().when(mockService).accept(eq("four-letter word"));
    kafkaTestUtils.sendMessage(topicName, "four-letter word");
    startConsumerWithRetries();
    waitUntil(() -> {
      checkPartitionsAssigned();
      verify(mockService, times(2)).accept(eq("first pancake"));
      verify(mockService, times(2)).accept(eq("two left feet"));
      verify(mockService, times(2)).accept(eq("three know it"));
      verify(mockService, times(1)).accept(eq("four-letter word"));
    });
    assertEquals(3, countMessagesInTopic(getDefaultRetryTopic()));
  }

  @Test
  void retryFailsInConsumerWithAllPartitionsAssigned() throws InterruptedException {
    kafkaTestUtils.sendMessage(topicName, "first pancake");
    AtomicBoolean exceptionThrown = new AtomicBoolean(false);
    consumer = consumerFactory
        .builder(topicName, String.class)
        .withOperationName("testOperation")
        .withConsumeStrategy((messages, ack) -> {
          try {
            ack.retry(messages.get(0), null);
          } catch (UnsupportedOperationException unsupported) {
            exceptionThrown.set(true);
          }
        })
        .withAllPartitionsAssigned(SeekPosition.EARLIEST)
        .build();
    consumer.start();
    waitUntil(() -> {
      assertEquals(5, consumer.getAssignedPartitions().size());
      assertTrue(exceptionThrown.get());
    });
  }

  @Test
  void retryConsumerReadsFromRetryReceiveTopic() throws InterruptedException {
    startConsumerWithRetries();
    waitUntil(() -> {
      checkPartitionsAssigned();
    });
    kafkaTestUtils.sendMessage(getDefaultRetryTopic(), "retry message");
    waitUntil(() -> {
      verify(mockService, times(1)).accept(eq("retry message"));
    });
  }

  @Test
  void retryConsumerRestartsAlongWithMainConsumer() {
    startConsumerWithRetries();
    assertTrue(consumer.isRunning());
    assertTrue(consumer.retryKafkaConsumer.isRunning());
    consumer.stop();
    assertFalse(consumer.isRunning());
    assertFalse(consumer.retryKafkaConsumer.isRunning());
    consumer.start();
    assertTrue(consumer.isRunning());
    assertTrue(consumer.retryKafkaConsumer.isRunning());
  }

  @Test
  void retryConsumerStopsWithCallbackAlongWithMainConsumer() throws InterruptedException {
    startConsumerWithRetries();
    assertTrue(consumer.isRunning());
    assertTrue(consumer.retryKafkaConsumer.isRunning());
    AtomicInteger callbackCalls = new AtomicInteger(0);
    consumer.stop(callbackCalls::incrementAndGet);
    waitUntil(() -> assertEquals(1, callbackCalls.get()));
    assertFalse(consumer.isRunning());
    assertFalse(consumer.retryKafkaConsumer.isRunning());
  }

  private void checkPartitionsAssigned() {
    assertEquals(5, consumer.getAssignedPartitions().size());
    assertEquals(5, consumer.retryKafkaConsumer.getAssignedPartitions().size());
  }

  private void startConsumerWithRetries() {
    ConsumeStrategy<String> consumeStrategy = ConsumeStrategy.atLeastOnceWithBatchAck(mockService::accept);
    consumer = consumerFactory
        .builder(topicName, String.class)
        .withOperationName("testOperation")
        .withConsumeStrategy(consumeStrategy)
        .withRetries(retryProducer, RetryPolicyResolver.always(RetryPolicy.fixed(Duration.ofSeconds(1))))
        // reduce retry consumer sleep duration to speed-up tests
        .withRetryConsumeStrategy(ConsumerBuilder.decorateForDelayedRetry(consumeStrategy, Duration.ofSeconds(1)))
        .build();
    consumer.start();
  }

  private String getDefaultRetryTopic() {
    return RetryTopics.defaultRetryReceiveTopic(new ConsumerMetadata("service", topicName, "testOperation"));
  }

  private <T> CompletableFuture<KafkaSendResult<T>> sendToKafka(ProducerRecord<String, T> record) {
    try {
      kafkaTestUtils.sendMessage(toBinaryRecord(record)).get(); //TODO Add sending ProducerRecord with JSON value to kafka-test-utils
    } catch (InterruptedException | ExecutionException e) {
      return CompletableFuture.failedFuture(e);
    }
    return CompletableFuture.completedFuture(null);
  }

  private static <T> ProducerRecord<String, byte[]> toBinaryRecord(ProducerRecord<String, T> record)  {
    try {
      return new ProducerRecord<>(
          record.topic(),
          record.partition(),
          record.key(),
          KafkaTestConfig.OBJECT_MAPPER.writeValueAsBytes(record.value()),
          record.headers()
      );
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
