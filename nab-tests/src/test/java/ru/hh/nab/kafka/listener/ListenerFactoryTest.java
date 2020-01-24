package ru.hh.nab.kafka.listener;

import static org.awaitility.Awaitility.await;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.hh.kafka.test.TestKafkaWithJsonMessages;
import ru.hh.nab.kafka.KafkaTestConfig;
import ru.hh.nab.kafka.consumer.Listener;
import ru.hh.nab.kafka.consumer.ListenerFactory;
import javax.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@ContextConfiguration(classes = {KafkaTestConfig.class})
public class ListenerFactoryTest extends AbstractJUnit4SpringContextTests {

  @Inject
  private ListenerFactory listenerFactory;
  @Inject
  protected TestKafkaWithJsonMessages kafkaTestUtils;

  private TopicListenerMock<String> mockListener;
  private String topicName;
  private Listener listener;

  @Before
  public void setUp() {
    mockListener = new TopicListenerMock<>();
    topicName = UUID.randomUUID().toString();
  }

  @After
  public void tearDown() {
    listener.stopListen();
  }

  @Test
  public void shouldReceiveSingleMessageFromTopic() {
    listener = startListen(topicName, String.class, mockListener);

    String payload = "it's test message";
    kafkaTestUtils.sendMessage(topicName, payload);

    await()
        .atMost(5, TimeUnit.SECONDS)
        .untilAsserted(() -> mockListener.assertMessagesEquals(List.of(payload)));
  }

  @Test
  public void shouldReceiveMessageByMessageFromTopic() {
    listener = startListen(topicName, String.class, mockListener);

    String firstMessage = "1";
    kafkaTestUtils.sendMessage(topicName, firstMessage);
    await()
        .atMost(5, TimeUnit.SECONDS)
        .untilAsserted(() -> mockListener.assertMessagesEquals(List.of(firstMessage)));

    String secondMessage = "2";
    kafkaTestUtils.sendMessage(topicName, secondMessage);
    await()
        .atMost(5, TimeUnit.SECONDS)
        .untilAsserted(() -> mockListener.assertMessagesEquals(List.of(secondMessage)));
  }


  private <T> Listener startListen(String topicName, Class<T> messageClass, TopicListenerMock<T> mockListener) {
    var container = listenerFactory.listenTopic(topicName, "testOperation", messageClass, mockListener);

    await().atMost(10, TimeUnit.SECONDS)
        .until(() -> container.getAssignedPartitions().size() == 1);

    return container;
  }

}
