package ru.hh.nab.kafka.consumer;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.hh.kafka.test.TestKafkaWithJsonMessages;
import ru.hh.nab.kafka.KafkaTestConfig;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {KafkaTestConfig.class})
public abstract class KafkaConsumerTestbase {

  @Inject
  protected TestKafkaWithJsonMessages kafkaTestUtils;

  @Inject
  private KafkaConsumerFactory consumerFactory;

  protected String topicName;

  @BeforeEach
  public void initializeTopic() {
    topicName = UUID.randomUUID().toString();
  }

  protected <T> KafkaConsumer<T> startMessagesConsumer(Class<T> messageClass, ConsumeStrategy<T> consumerMock) {
    return startMessagesConsumer(messageClass, "testOperation", consumerMock);
  }

  protected <T> KafkaConsumer<T> startMessagesConsumer(Class<T> messageClass, String operation, ConsumeStrategy<T> consumerMock) {
    KafkaConsumer<T> consumer = consumerFactory.subscribe(topicName, "testOperation", messageClass, consumerMock);
    await().atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertEquals(5, consumer.getAssignedPartitions().size()));
    return consumer;
  }
}
