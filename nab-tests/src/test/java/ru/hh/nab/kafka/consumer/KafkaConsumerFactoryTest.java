package ru.hh.nab.kafka.consumer;

import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import ru.hh.nab.kafka.KafkaTestConfig;

@ContextConfiguration(classes = {KafkaTestConfig.class})
public class KafkaConsumerFactoryTest extends KafkaConsumerTestbase {

  protected TopicConsumerMock<String> consumerMock;
  protected KafkaConsumer<String> consumer;

  @Before
  public void setUp() {
    consumerMock = new TopicConsumerMock<>();
  }

  @After
  public void tearDown() {
    consumer.stop();
  }

  @Test
  public void shouldReceiveSingleMessageFromTopic() {
    consumer = startMessagesConsumer(String.class, consumerMock);

    String payload = "it's test message";
    kafkaTestUtils.sendMessage(topicName, payload);

    await()
        .atMost(5, TimeUnit.SECONDS)
        .untilAsserted(() -> consumerMock.assertMessagesEquals(List.of(payload)));
  }

  @Test
  public void shouldReceiveMessageByMessageFromTopic() {
    consumer = startMessagesConsumer(String.class, consumerMock);

    String firstMessage = "1";
    kafkaTestUtils.sendMessage(topicName, firstMessage);
    await()
        .atMost(5, TimeUnit.SECONDS)
        .untilAsserted(() -> consumerMock.assertMessagesEquals(List.of(firstMessage)));

    String secondMessage = "2";
    kafkaTestUtils.sendMessage(topicName, secondMessage);
    await()
        .atMost(5, TimeUnit.SECONDS)
        .untilAsserted(() -> consumerMock.assertMessagesEquals(List.of(secondMessage)));
  }

}
