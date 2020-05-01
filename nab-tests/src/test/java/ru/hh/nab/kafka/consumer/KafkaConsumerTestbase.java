package ru.hh.nab.kafka.consumer;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.hh.kafka.test.TestKafkaWithJsonMessages;

public abstract class KafkaConsumerTestbase extends AbstractJUnit4SpringContextTests {

  @Inject
  protected TestKafkaWithJsonMessages kafkaTestUtils;

  @Inject
  private KafkaConsumerFactory consumerFactory;

  protected String topicName;

  @Before
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
