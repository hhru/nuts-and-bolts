package ru.hh.nab.kafka.producer;

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
public class ProducerFactoryTest extends AbstractJUnit4SpringContextTests {

  @Inject
  private KafkaProducerFactory producerFactory;
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

    KafkaProducer<String> producer = producerFactory.createDefaultProducer();
    String testMessage = "payload";
    producer.sendMessage(topicName, testMessage);

    Optional<String> result = watcher.poolNextMessage();
    assertTrue(result.isPresent());
    assertEquals(testMessage, result.get());
  }

  @Test
  public void shouldPublishSeveralMessagesToTopic() {
    var watcher = kafkaTestUtils.startJsonTopicWatching(topicName, String.class);

    KafkaProducer<String> producer = producerFactory.createDefaultProducer();
    String testMessage = "payload";
    producer.sendMessage(topicName, testMessage);
    String testMessage2 = "payload2";
    producer.sendMessage(topicName, testMessage2);

    List<String> result = watcher.poolNextMessages();
    assertEquals(List.of(testMessage, testMessage2), result);
  }

}
