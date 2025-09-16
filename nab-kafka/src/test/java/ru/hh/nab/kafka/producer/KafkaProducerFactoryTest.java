package ru.hh.nab.kafka.producer;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.hh.kafka.test.KafkaTestUtils;
import ru.hh.kafka.test.TestKafkaWithJsonMessages;
import ru.hh.nab.kafka.KafkaTestConfig;

@SpringBootTest(classes = KafkaTestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class KafkaProducerFactoryTest {

  @Inject
  protected TestKafkaWithJsonMessages testKafka;
  @Inject
  private KafkaProducerFactory producerFactory;
  private String topicName;

  @BeforeEach
  public void createTempTopic() throws ExecutionException, InterruptedException {
    topicName = KafkaTestUtils.createTopic(testKafka.getBootstrapServers());
  }

  @Test
  public void shouldPublishMessageToTopic() throws ExecutionException, InterruptedException {
    var watcher = testKafka.startJsonTopicWatching(topicName, String.class);

    KafkaProducer producer = producerFactory.createDefaultProducer();
    String testMessage = "payload";
    KafkaSendResult<String> sendResult = producer.sendMessage(topicName, testMessage).get();
    assertEquals(testMessage, sendResult.getProducerRecord().value(), "Sent message differs from initial message");

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
    assertEquals(testMessage, sendResult.getProducerRecord().value(), "Sent message differs from initial message");
    String testMessage2 = "payload2";
    KafkaSendResult<String> sendResult2 = producer.sendMessage(topicName, testMessage2).get();
    assertEquals(testMessage2, sendResult2.getProducerRecord().value(), "Sent message differs from initial message");

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
    assertEquals(testMessage, sendResult.getProducerRecord().value(), "Sent message differs from initial message");

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
