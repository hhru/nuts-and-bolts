package ru.hh.nab.kafka.consumer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewPartitions;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.hh.kafka.test.TestKafkaWithJsonMessages;
import static ru.hh.nab.common.util.ExceptionUtils.getOrThrow;
import ru.hh.nab.kafka.KafkaTestConfig;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {KafkaTestConfig.class})
public abstract class KafkaConsumerTestbase {

  @Inject
  protected TestKafkaWithJsonMessages kafkaTestUtils;

  @Inject
  protected KafkaConsumerFactory consumerFactory;

  protected String topicName;

  @BeforeEach
  public void initializeTopic() {
    topicName = UUID.randomUUID().toString();
  }

  protected <T> KafkaConsumer<T> startMessagesConsumer(Class<T> messageClass, ConsumeStrategy<T> consumerMock) {
    return startMessagesConsumer(messageClass, "testOperation", consumerMock);
  }

  protected <T> KafkaConsumer<T> startMessagesConsumer(Class<T> messageClass, String operation, ConsumeStrategy<T> consumerMock) {
    KafkaConsumer<T> consumer = consumerFactory
        .builder(topicName, messageClass)
        .withOperationName(operation)
        .withConsumeStrategy(consumerMock)
        .start();
    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertEquals(5, consumer.getAssignedPartitions().size()));
    return consumer;
  }

  public void addPartitions(String topic, int finalPartitionsCount) throws InterruptedException, ExecutionException {
    AdminClient adminClient = kafkaTestUtils.getAdminClient();
    adminClient.createPartitions(Map.of(topic, NewPartitions.increaseTo(finalPartitionsCount))).all().get();
    waitUntil(() -> assertEquals(
            finalPartitionsCount,
            getOrThrow(() -> adminClient.describeTopics(List.of(topic)).values().get(topic).get().partitions().size())
        )
    );
  }

  protected void waitUntil(Runnable assertion) throws InterruptedException {
    await().atMost(10, TimeUnit.SECONDS).untilAsserted(assertion::run);
  }
}
