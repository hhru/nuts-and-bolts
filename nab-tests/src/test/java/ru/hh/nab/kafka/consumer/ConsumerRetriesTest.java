package ru.hh.nab.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hh.nab.kafka.KafkaTestConfig;
import ru.hh.nab.kafka.consumer.retry.RetryPolicyResolver;
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
    consumer = consumerFactory
        .builder(topicName, String.class)
        .withOperationName("testOperation")
        .withConsumeStrategy(ConsumeStrategy.atLeastOnceWithBatchAck(mockService::accept))
        .withRetries(retryProducer, RetryPolicyResolver.always(RetryPolicy.fixedDelay(Duration.ofSeconds(1))), true)
        .start();
    waitUntil(() -> {
      assertEquals(5, consumer.getAssignedPartitions().size());
      assertEquals(5, consumer.retryKafkaConsumer.getAssignedPartitions().size());
      verify(mockService, times(2)).accept(eq("first pancake"));
    });
  }

  private <T> CompletableFuture<KafkaSendResult<T>> sendToKafka(ProducerRecord<String, T> record) {
    try {
      kafkaTestUtils.sendMessage(toBinaryRecord(record)).get(); //TODO Add sending ProducerRecord with JSON value to kafka-test-utils
    } catch (InterruptedException | ExecutionException e) {
      return CompletableFuture.failedFuture(e);
    }
    return CompletableFuture.completedFuture(null);
  }

  @Test
  void offsetsNotCommitedUntilRetryMessagesAreSent() {

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
