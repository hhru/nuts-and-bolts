package ru.hh.nab.kafka.consumer;

import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class KafkaConsumerFactoryTest extends KafkaConsumerTestBase {
  private TopicConsumerMock<String> consumerMock;
  private KafkaConsumer<String> consumer;

  @BeforeEach
  public void setUp() {
    consumerMock = new TopicConsumerMock<>();
  }

  @AfterEach
  public void tearDown() {
    consumer.stop();
  }

  @Test
  public void shouldReceiveSingleMessageFromTopic() {
    consumer = startMessagesConsumer(String.class, consumerMock);

    String payload = "it's test message";
    testKafka.sendMessage(topicName, payload);

    await()
        .atMost(5, TimeUnit.SECONDS)
        .untilAsserted(() -> consumerMock.assertMessagesEquals(List.of(payload)));
  }

  @Test
  public void shouldReceiveMessageByMessageFromTopic() {
    consumer = startMessagesConsumer(String.class, consumerMock);

    String firstMessage = "1";
    testKafka.sendMessage(topicName, firstMessage);
    await()
        .atMost(5, TimeUnit.SECONDS)
        .untilAsserted(() -> consumerMock.assertMessagesEquals(List.of(firstMessage)));

    String secondMessage = "2";
    testKafka.sendMessage(topicName, secondMessage);
    await()
        .atMost(5, TimeUnit.SECONDS)
        .untilAsserted(() -> consumerMock.assertMessagesEquals(List.of(secondMessage)));
  }
}
