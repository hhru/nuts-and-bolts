package ru.hh.nab.kafka.publisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.hh.kafka.test.TestKafkaWithJsonMessages;
import ru.hh.nab.kafka.KafkaTestConfig;
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ContextConfiguration(classes = {KafkaTestConfig.class})
public class PublisherFactoryTest extends AbstractJUnit4SpringContextTests {

  @Inject
  private PublisherFactory publisherFactory;
  @Inject
  protected TestKafkaWithJsonMessages kafkaTestUtils;

  private String topicName;

  @Before
  public void createTempTopic() {
    topicName = UUID.randomUUID().toString();
  }

  @Test
  public void shouldPublishMessageToTopic() {
    var watcher = kafkaTestUtils.startJsonTopicWatching(topicName, String.class);

    Publisher<String> publisher = publisherFactory.createForTopic(topicName);
    String testMessage = "payload";
    publisher.sendMessage(testMessage);

    Optional<String> result = watcher.poolNextMessage();
    assertTrue(result.isPresent());
    assertEquals(testMessage, result.get());
  }

  @Test
  public void shouldPublishSeveralMessagesToTopic() {
    var watcher = kafkaTestUtils.startJsonTopicWatching(topicName, String.class);

    Publisher<String> publisher = publisherFactory.createForTopic(topicName);
    String testMessage = "payload";
    publisher.sendMessage(testMessage);
    String testMessage2 = "payload2";
    publisher.sendMessage(testMessage2);

    List<String> result = watcher.poolNextMessages();
    assertEquals(List.of(testMessage, testMessage2), result);
  }

}
