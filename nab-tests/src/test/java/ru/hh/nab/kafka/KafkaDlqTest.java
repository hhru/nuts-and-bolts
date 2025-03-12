package ru.hh.nab.kafka;

import java.time.Duration;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.awaitility.Awaitility;
import static org.awaitility.Awaitility.await;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.hh.kafka.test.TestKafkaWithJsonMessages;
import ru.hh.nab.kafka.consumer.ConsumeStrategy;
import ru.hh.nab.kafka.consumer.KafkaConsumer;
import ru.hh.nab.kafka.consumer.KafkaConsumerFactory;
import ru.hh.nab.kafka.consumer.retry.RetryPolicyResolver;
import ru.hh.nab.kafka.consumer.retry.RetryTopics;
import ru.hh.nab.kafka.consumer.retry.policy.RetryPolicy;
import ru.hh.nab.kafka.producer.KafkaProducer;
import ru.hh.nab.kafka.producer.KafkaProducerFactory;

@Tag("integration")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {KafkaTestConfig.class})
public class KafkaDlqTest {

  @Inject
  private TestKafkaWithJsonMessages kafkaTestUtils;

  @Inject
  private KafkaConsumerFactory kafkaConsumerFactory;

  @Inject
  private KafkaProducerFactory kafkaProducerFactory;

  /**
   * Verifies single message is sent to DLQ topic after sendToDlq operation
   */

  @Test
  public void testSendToDlqSingleMessage() {
    AdminClient adminClient = kafkaTestUtils.getAdminClient();
    NewTopic originalTopic = createRandomTopic();
    NewTopic dlqTopic = createRandomTopic();
    adminClient.createTopics(List.of(originalTopic, dlqTopic));

    String originalMessage = "original message";
    kafkaTestUtils.sendMessage(originalTopic.name(), originalMessage);

    AtomicBoolean isReceivedFromDlq = new AtomicBoolean(false);
    // Size of 2 used to verify that only 1 message will be consumed at the end
    Queue<String> receivedMessages = new ArrayBlockingQueue<>(2);
    KafkaConsumer<String> dlqConsumer = startConsumer(createRandomId(), dlqTopic.name(), String.class, (messages, ack) -> {
      for (ConsumerRecord<String, String> message : messages) {
        receivedMessages.add(message.value());
      }
      ack.acknowledge();
      isReceivedFromDlq.set(true);
    });

    // Test
    AtomicBoolean isSentToDlq = new AtomicBoolean(false);
    KafkaConsumer<String> originalConsumer = startConsumer(
        createRandomId(),
        originalTopic.name(),
        dlqTopic.name(),
        String.class,
        (messages, ack) -> {
          for (ConsumerRecord<String, String> message : messages) {
            // explicitly send to DLQ
            ack.sendToDlq(message);
            // await sendToDlq operation
            ack.acknowledge(message);
          }
          isSentToDlq.set(true);
        }
    );

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilTrue(isSentToDlq);
    Awaitility.await().atMost(Duration.ofSeconds(2)).untilTrue(isReceivedFromDlq);

    Assertions.assertEquals(List.of(originalMessage).size(), receivedMessages.size());
    Assertions.assertEquals(originalMessage, receivedMessages.poll());

    // Tests will share kafka container, do not forget to perform resources cleanup
    dlqConsumer.stop();
    originalConsumer.stop();
    adminClient.deleteTopics(List.of(originalTopic.name(), dlqTopic.name()));
  }

  /**
   * Verifies single message is sent to DLQ topic after retry operation with sendToDlq
   */

  @Test
  public void testRetryAndSendToDlqSingleMessage() {
    AdminClient adminClient = kafkaTestUtils.getAdminClient();
    NewTopic originalTopic = createRandomTopic();
    NewTopic retryTopic = createRandomTopic();
    NewTopic dlqTopic = createRandomTopic();
    adminClient.createTopics(List.of(originalTopic, retryTopic, dlqTopic));

    String originalMessage = "original message";
    kafkaTestUtils.sendMessage(originalTopic.name(), originalMessage);

    AtomicBoolean isReceivedFromDlq = new AtomicBoolean(false);
    // Size of 2 used to verify that only 1 message will be consumed at the end
    Queue<String> receivedMessages = new ArrayBlockingQueue<>(2);
    KafkaConsumer<String> dlqConsumer = startConsumer(createRandomId(), dlqTopic.name(), String.class, (messages, ack) -> {
      for (ConsumerRecord<String, String> message : messages) {
        receivedMessages.add(message.value());
      }
      ack.acknowledge();
      isReceivedFromDlq.set(true);
    });

    AtomicBoolean isReceivedFromRetry = new AtomicBoolean(false);
    KafkaConsumer<String> retryConsumer = startConsumer(
        createRandomId(),
        retryTopic.name(),
        dlqTopic.name(),
        String.class,
        (messages, ack) -> {
          ack.acknowledge();
          isReceivedFromRetry.set(true);
        }
    );

    // Test
    AtomicBoolean isSentToRetry = new AtomicBoolean(false);
    RetryPolicyResolver<String> policy = RetryPolicyResolver.always(RetryPolicy.fixed(Duration.ofSeconds(1)));
    KafkaConsumer<String> originalConsumer = startConsumer(
        createRandomId(),
        originalTopic.name(),
        retryTopic.name(),
        policy,
        dlqTopic.name(),
        String.class,
        (messages, ack) -> {
          for (ConsumerRecord<String, String> message : messages) {
            ack.retry(message, new TimeoutException("Stub exception"));
            // explicitly send to DLQ and await retry operation completion
            ack.sendToDlq(message);
            // await sendToDlq operation
            ack.acknowledge(message);
          }
          isSentToRetry.set(true);
        }
    );

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilTrue(isSentToRetry);
    Awaitility.await().atMost(Duration.ofSeconds(2)).untilTrue(isReceivedFromRetry);
    Awaitility.await().atMost(Duration.ofSeconds(2)).untilTrue(isReceivedFromDlq);

    Assertions.assertEquals(List.of(originalMessage).size(), receivedMessages.size());
    Assertions.assertEquals(originalMessage, receivedMessages.poll());

    // Tests will share kafka container, do not forget to perform resources cleanup
    dlqConsumer.stop();
    retryConsumer.stop();
    originalConsumer.stop();
    adminClient.deleteTopics(List.of(originalTopic.name(), retryTopic.name(), dlqTopic.name()));
  }

  /**
   * Verifies single message is sent to DLQ topic after retry operation limits were exhausted
   */

  @Test
  public void testRetryExhaustAndSendToDlqSingleMessage() {
    AdminClient adminClient = kafkaTestUtils.getAdminClient();
    NewTopic originalTopic = createRandomTopic();
    NewTopic retryTopic = createRandomTopic();
    NewTopic dlqTopic = createRandomTopic();
    adminClient.createTopics(List.of(originalTopic, retryTopic, dlqTopic));

    String originalMessage = "original message";
    kafkaTestUtils.sendMessage(originalTopic.name(), originalMessage);

    AtomicBoolean isReceivedFromDlq = new AtomicBoolean(false);
    // Size of 2 used to verify that only 1 message will be consumed at the end
    Queue<String> receivedMessages = new ArrayBlockingQueue<>(2);
    KafkaConsumer<String> dlqConsumer = startConsumer(createRandomId(), dlqTopic.name(), String.class, (messages, ack) -> {
      for (ConsumerRecord<String, String> message : messages) {
        receivedMessages.add(message.value());
      }
      ack.acknowledge();
      isReceivedFromDlq.set(true);
    });

    AtomicBoolean isReceivedFromRetry = new AtomicBoolean(false);
    KafkaConsumer<String> retryConsumer = startConsumer(
        createRandomId(),
        retryTopic.name(),
        dlqTopic.name(),
        String.class,
        (messages, ack) -> {
          ack.acknowledge();
          isReceivedFromRetry.set(true);
        }
    );

    // Test
    AtomicBoolean isSentToRetry = new AtomicBoolean(false);
    RetryPolicyResolver<String> policy = RetryPolicyResolver.always(RetryPolicy.fixed(Duration.ofSeconds(1)).withRetryLimit(1L));
    KafkaConsumer<String> originalConsumer = startConsumer(
        createRandomId(),
        originalTopic.name(),
        retryTopic.name(),
        policy,
        dlqTopic.name(),
        String.class,
        (messages, ack) -> {
          for (ConsumerRecord<String, String> message : messages) {
            ack.retry(message, new TimeoutException("Stub exception"));
            // explicitly await retry budget exhaustion
            ack.acknowledge(message);
          }
          isSentToRetry.set(true);
        }
    );

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilTrue(isSentToRetry);
    Awaitility.await().atMost(Duration.ofSeconds(2)).untilTrue(isReceivedFromRetry);
    Awaitility.await().atMost(Duration.ofSeconds(2)).untilTrue(isReceivedFromDlq);

    Assertions.assertEquals(List.of(originalMessage).size(), receivedMessages.size());
    Assertions.assertEquals(originalMessage, receivedMessages.poll());

    // Tests will share kafka container, do not forget to perform resources cleanup
    dlqConsumer.stop();
    retryConsumer.stop();
    originalConsumer.stop();
    adminClient.deleteTopics(List.of(originalTopic.name(), retryTopic.name(), dlqTopic.name()));
  }

  /**
   * Verifies specific messages are sent to DLQ topic after sendToDlq operation
   */

  @Test
  public void testSendToDlqSeveralMessages() {
    AdminClient adminClient = kafkaTestUtils.getAdminClient();
    NewTopic originalTopic = createRandomTopic();
    NewTopic dlqTopic = createRandomTopic();
    adminClient.createTopics(List.of(originalTopic, dlqTopic));

    String dlqMessageFirst = "first message";
    kafkaTestUtils.sendMessage(originalTopic.name(), dlqMessageFirst);
    String ackMessage = "second message";
    kafkaTestUtils.sendMessage(originalTopic.name(), ackMessage);
    String dlqMessageSecond = "third message";
    kafkaTestUtils.sendMessage(originalTopic.name(), dlqMessageSecond);
    List<String> dlqMessages = List.of(dlqMessageFirst, dlqMessageSecond);

    AtomicBoolean isReceivedFromDlq = new AtomicBoolean(false);
    // Size of 3 used to verify that only 2 messages will be consumed at the end
    Queue<String> receivedMessages = new ArrayBlockingQueue<>(3);
    KafkaConsumer<String> dlqConsumer = startConsumer(createRandomId(), dlqTopic.name(), String.class, (messages, ack) -> {
      for (ConsumerRecord<String, String> message : messages) {
        receivedMessages.add(message.value());
      }
      ack.acknowledge();
      isReceivedFromDlq.set(true);
    });

    // Test
    AtomicBoolean isSentToRetry = new AtomicBoolean(false);
    KafkaConsumer<String> originalConsumer = startConsumer(
        createRandomId(),
        originalTopic.name(),
        dlqTopic.name(),
        String.class,
        (messages, ack) -> {

          List<ConsumerRecord<String, String>> messagesSendToDlq = messages
              .stream()
              .filter(msg -> dlqMessages.contains(msg.value()))
              .toList();
          ack.sendToDlq(messagesSendToDlq);
          // await sendToDlq operation
          ack.acknowledge();

          isSentToRetry.set(true);
        }
    );

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilTrue(isSentToRetry);
    Awaitility.await().atMost(Duration.ofSeconds(2)).untilTrue(isReceivedFromDlq);

    Assertions.assertEquals(dlqMessages.size(), receivedMessages.size());
    Assertions.assertTrue(receivedMessages.containsAll(dlqMessages));

    // Tests will share kafka container, do not forget to perform resources cleanup
    dlqConsumer.stop();
    originalConsumer.stop();
    adminClient.deleteTopics(List.of(originalTopic.name(), dlqTopic.name()));
  }

  /**
   * Verifies all messages are sent to DLQ topic after sendToDlq operation
   */

  @Test
  public void testSendToDlqAllMessages() {
    AdminClient adminClient = kafkaTestUtils.getAdminClient();
    NewTopic originalTopic = createRandomTopic();
    NewTopic dlqTopic = createRandomTopic();
    adminClient.createTopics(List.of(originalTopic, dlqTopic));

    String dlqMessageFirst = "first message";
    kafkaTestUtils.sendMessage(originalTopic.name(), dlqMessageFirst);
    String dlqMessageSecond = "second message";
    kafkaTestUtils.sendMessage(originalTopic.name(), dlqMessageSecond);
    List<String> dlqMessages = List.of(dlqMessageFirst, dlqMessageSecond);

    AtomicBoolean isReceivedFromDlq = new AtomicBoolean(false);
    // Size of 3 used to verify that only 2 messages will be consumed at the end
    Queue<String> receivedMessages = new ArrayBlockingQueue<>(3);
    KafkaConsumer<String> dlqConsumer = startConsumer(createRandomId(), dlqTopic.name(), String.class, (messages, ack) -> {
      for (ConsumerRecord<String, String> message : messages) {
        receivedMessages.add(message.value());
      }
      ack.acknowledge();
      isReceivedFromDlq.set(true);
    });

    // Test
    AtomicBoolean isSentToRetry = new AtomicBoolean(false);
    KafkaConsumer<String> originalConsumer = startConsumer(
        createRandomId(),
        originalTopic.name(),
        dlqTopic.name(),
        String.class,
        (messages, ack) -> {
          ack.sendToDlq(messages);
          // await sendToDlq operation
          ack.acknowledge(messages);
          isSentToRetry.set(true);
        }
    );

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilTrue(isSentToRetry);
    Awaitility.await().atMost(Duration.ofSeconds(2)).untilTrue(isReceivedFromDlq);

    Assertions.assertEquals(dlqMessages.size(), receivedMessages.size());
    Assertions.assertTrue(receivedMessages.containsAll(dlqMessages));

    // Tests will share kafka container, do not forget to perform resources cleanup
    dlqConsumer.stop();
    originalConsumer.stop();
    adminClient.deleteTopics(List.of(originalTopic.name(), dlqTopic.name()));
  }

  /**
   * Verifies malformed message is skipped and processing continues
   */

  @Test
  public void testSendToDlqWithMalformedMessage() {
    AdminClient adminClient = kafkaTestUtils.getAdminClient();
    NewTopic originalTopic = createRandomTopic();
    NewTopic dlqTopic = createRandomTopic();
    adminClient.createTopics(List.of(originalTopic, dlqTopic));

    record InvalidMessage(String msg, int status) {
    }
    record ValidMessage(String msg) {
    }

    String originalMessage = "original message";
    // send message with unexpected format
    kafkaTestUtils.sendMessage(originalTopic.name(), new InvalidMessage(originalMessage, 42));
    // send message with expected format
    ValidMessage validMessage = new ValidMessage(originalMessage);
    kafkaTestUtils.sendMessage(originalTopic.name(), validMessage);

    // Test
    AtomicBoolean isReceived = new AtomicBoolean(false);
    // Size of 2 used to verify that only 1 message will be consumed at the end
    Queue<ValidMessage> receivedMessages = new ArrayBlockingQueue<>(2);
    KafkaConsumer<ValidMessage> originalConsumer = startConsumer(
        createRandomId(),
        originalTopic.name(),
        dlqTopic.name(),
        ValidMessage.class,
        (messages, ack) -> {
          for (ConsumerRecord<String, ValidMessage> message : messages) {
            receivedMessages.add(message.value());
            ack.sendToDlq(message);
            // await sendToDlq operation
            ack.acknowledge(message);
          }
          isReceived.set(true);
        }
    );

    Awaitility.await().atMost(Duration.ofSeconds(2)).untilTrue(isReceived);

    Assertions.assertEquals(List.of(validMessage).size(), receivedMessages.size());
    Assertions.assertEquals(validMessage, receivedMessages.poll());

    // Tests will share kafka container, do not forget to perform resources cleanup
    originalConsumer.stop();
    adminClient.deleteTopics(List.of(originalTopic.name(), dlqTopic.name()));
  }

  private <T> KafkaConsumer<T> startConsumer(
      String clientId,
      String topicName,
      String retryTopicName,
      RetryPolicyResolver<T> retryPolicyResolver,
      String dlqTopicName,
      Class<T> messageClass,
      ConsumeStrategy<T> consumerMock
  ) {
    KafkaProducer defaultProducer = kafkaProducerFactory.createDefaultProducer();
    KafkaConsumer<T> consumer = kafkaConsumerFactory
        .builder(topicName, messageClass)
        .withClientId(clientId)
        .withConsumerGroup()
        .withOperationName(createRandomId())
        .withRetries(defaultProducer, retryPolicyResolver, new RetryTopics(retryTopicName, retryTopicName))
        .withDlq(dlqTopicName, defaultProducer)
        .withConsumeStrategy(consumerMock)
        .build();

    consumer.start();

    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertFalse(consumer.getAssignedPartitions().isEmpty()));

    return consumer;
  }

  private <T> KafkaConsumer<T> startConsumer(
      String clientId, String topicName, String dlqTopicName, Class<T> messageClass, ConsumeStrategy<T> consumerMock
  ) {
    KafkaConsumer<T> consumer = kafkaConsumerFactory
        .builder(topicName, messageClass)
        .withClientId(clientId)
        .withConsumerGroup()
        .withOperationName(createRandomId())
        .withDlq(dlqTopicName, kafkaProducerFactory.createDefaultProducer())
        .withConsumeStrategy(consumerMock)
        .build();

    consumer.start();

    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertFalse(consumer.getAssignedPartitions().isEmpty()));

    return consumer;
  }

  private <T> KafkaConsumer<T> startConsumer(
      String clientId, String topicName, Class<T> messageClass, ConsumeStrategy<T> consumerMock
  ) {
    KafkaConsumer<T> consumer = kafkaConsumerFactory
        .builder(topicName, messageClass)
        .withClientId(clientId)
        .withConsumerGroup()
        .withOperationName(createRandomId())
        .withConsumeStrategy(consumerMock)
        .build();

    consumer.start();

    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertFalse(consumer.getAssignedPartitions().isEmpty()));

    return consumer;
  }

  private static NewTopic createRandomTopic() {
    return new NewTopic(createRandomId(), 1, (short) 1);
  }

  private static String createRandomId() {
    return UUID.randomUUID().toString();
  }
}
