package ru.hh.nab.kafka.consumer;

import static org.awaitility.Awaitility.await;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.hh.kafka.test.TestKafkaWithJsonMessages;
import ru.hh.nab.kafka.KafkaTestConfig;
import javax.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@ContextConfiguration(classes = {KafkaTestConfig.class})
public class KafkaConsumerFactoryTest extends AbstractJUnit4SpringContextTests {

  @Inject
  private KafkaConsumerFactory consumerFactory;
  @Inject
  protected TestKafkaWithJsonMessages kafkaTestUtils;

  private TopicConsumerMock<String> consumerMock;
  private String topicName;
  private KafkaConsumer consumer;

  @Before
  public void setUp() {
    consumerMock = new TopicConsumerMock<>();
    topicName = UUID.randomUUID().toString();
  }

  @After
  public void tearDown() {
    consumer.stopConsumer();
  }

  @Test
  public void shouldReceiveSingleMessageFromTopic() {
    consumer = startConsumeMessages(topicName, String.class, consumerMock);

    String payload = "it's test message";
    kafkaTestUtils.sendMessage(topicName, payload);

    await()
        .atMost(5, TimeUnit.SECONDS)
        .untilAsserted(() -> consumerMock.assertMessagesEquals(List.of(payload)));
  }

  @Test
  public void shouldReceiveMessageByMessageFromTopic() {
    consumer = startConsumeMessages(topicName, String.class, consumerMock);

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


  private <T> KafkaConsumer startConsumeMessages(String topicName, Class<T> messageClass, TopicConsumerMock<T> consumerMock) {
    var container = consumerFactory.subscribe(topicName, "testOperation", messageClass, consumerMock);

    await().atMost(10, TimeUnit.SECONDS)
        .until(() -> container.getAssignedPartitions().size() == 1);

    return container;
  }

}
