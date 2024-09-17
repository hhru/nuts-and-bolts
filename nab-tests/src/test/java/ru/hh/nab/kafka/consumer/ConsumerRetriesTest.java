package ru.hh.nab.kafka.consumer;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import static java.util.Collections.synchronizedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.apache.kafka.clients.producer.ProducerRecord;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static ru.hh.nab.kafka.consumer.retry.HeadersMessageMetadataProvider.getMessageProcessingHistory;
import static ru.hh.nab.kafka.consumer.retry.HeadersMessageMetadataProvider.getNextRetryTime;
import ru.hh.nab.kafka.consumer.retry.MessageProcessingHistory;
import ru.hh.nab.kafka.consumer.retry.RetryPolicyResolver;
import ru.hh.nab.kafka.consumer.retry.policy.RetryPolicy;
import ru.hh.nab.kafka.producer.KafkaProducer;
import ru.hh.nab.kafka.producer.KafkaSendResult;

public class ConsumerRetriesTest extends KafkaConsumerTestbase {
  private List<ProducerRecord<String, String>> producedMessages;
  private List<String> processedMessages;

  KafkaProducer retryProducer = new KafkaProducer() {
    @Override
    public <T> CompletableFuture<KafkaSendResult<T>> sendMessage(ProducerRecord<String, T> record, Executor executor) {
      producedMessages.add((ProducerRecord<String, String>) record);
      return CompletableFuture.completedFuture(null);
    }
  };

  @BeforeEach
  void setUp() {
    producedMessages = synchronizedList(new ArrayList<>());
    processedMessages = synchronizedList(new ArrayList<>());
  }

  @Test
  void firstRetryAddsHeaders() throws InterruptedException {
    kafkaTestUtils.sendMessage(topicName, "good");
    kafkaTestUtils.sendMessage(topicName, "bad");
    kafkaTestUtils.sendMessage(topicName, "ugly");

    KafkaConsumer<String> consumer = consumerFactory
        .builder(topicName, String.class)
        .withOperationName("testOperation")
        .withConsumeStrategy(ConsumeStrategy.atLeastOnceWithBatchAck(message -> {
          if (message.equals("bad")) {
            throw new RuntimeException();
          }
          processedMessages.add(message);
        }))
        .withStandaloneRetries(
            retryProducer,
            RetryPolicyResolver.always(RetryPolicy.fixedDelay(Duration.ofSeconds(10))))
        .start();

    waitUntil(() -> {
      assertEquals(5, consumer.getAssignedPartitions().size());
      assertEquals(3, processedMessages.size() + producedMessages.size());
    });
    consumer.stop();

    assertEquals("good", processedMessages.get(0));
    assertEquals("ugly", processedMessages.get(1));
    ProducerRecord<String, String> retryMessage = producedMessages.get(0);
    assertEquals("bad", retryMessage.value());
    MessageProcessingHistory history = getMessageProcessingHistory(retryMessage.headers()).get();
    assertEquals(1, history.retryNumber());
    Instant nextRetryTime = getNextRetryTime(retryMessage.headers()).get();
    assertEquals(history.lastFailTime().plusSeconds(10), nextRetryTime);
  }


}
