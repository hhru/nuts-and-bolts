package ru.hh.nab.kafka.producer;

import java.util.Objects;
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
import java.util.concurrent.ExecutionException;

@ContextConfiguration(classes = {KafkaTestConfig.class})
public class ProducerFactoryTest extends AbstractJUnit4SpringContextTests {

  @Inject
  private KafkaProducerFactory producerFactory;
  @Inject
  protected TestKafkaWithJsonMessages testKafka;

  private String topicName;

  @Before
  public void createTempTopic() {
    topicName = UUID.randomUUID().toString();
  }

  @Test
  public void shouldPublishMessageToTopic() throws ExecutionException, InterruptedException {
    var watcher = testKafka.startJsonTopicWatching(topicName, String.class);

    KafkaProducer producer = producerFactory.createDefaultProducer();
    String testMessage = "payload";
    KafkaSendResult<String> sendResult = producer.sendMessage(topicName, testMessage).get();
    assertEquals("Sent message differs from initial message", testMessage, sendResult.getProducerRecord().value());

    Optional<String> result = watcher.poolNextMessage();
    assertTrue(result.isPresent());
    assertEquals(testMessage, result.get());
  }

  @Test
  public void shouldPublishSeveralMessagesToTopic() throws ExecutionException, InterruptedException {
    var watcher = testKafka.startJsonTopicWatching(topicName, String.class);

    KafkaProducer producer = producerFactory.createDefaultProducer();
    String testMessage = "payload";
    KafkaSendResult<String> sendResult = producer.sendMessage(topicName, testMessage).get();
    assertEquals("Sent message differs from initial message", testMessage, sendResult.getProducerRecord().value());
    String testMessage2 = "payload2";
    KafkaSendResult<String> sendResult2 = producer.sendMessage(topicName, testMessage2).get();
    assertEquals("Sent message differs from initial message", testMessage2, sendResult2.getProducerRecord().value());

    List<String> result = watcher.poolNextMessages();
    assertEquals(2, result.size());
    assertTrue(result.contains(testMessage));
    assertTrue(result.contains(testMessage2));
  }

  @Test
  public void testJacksonDtoPublish() throws ExecutionException, InterruptedException {
    var watcher = testKafka.startJsonTopicWatching(topicName, TestDto.class);

    KafkaProducer producer = producerFactory.createDefaultProducer();
    TestDto testMessage = new TestDto("228", "test");
    KafkaSendResult<TestDto> sendResult = producer.sendMessage(topicName, testMessage).get();
    assertEquals("Sent message differs from initial message", testMessage, sendResult.getProducerRecord().value());

    List<TestDto> result = watcher.poolNextMessages();
    assertEquals(1, result.size());
    assertTrue(result.contains(testMessage));
  }

  private static class TestDto {

    public String key;
    public String value;

    public TestDto() {
    }

    public TestDto(String key, String value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      TestDto testDto = (TestDto) o;
      return key.equals(testDto.key) &&
          value.equals(testDto.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(key, value);
    }
  }
}
