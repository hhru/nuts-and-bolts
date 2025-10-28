package ru.hh.nab.kafka.consumer;

import jakarta.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import static java.util.stream.Collectors.toMap;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import ru.hh.kafka.test.TestKafkaWithJsonMessages;
import static ru.hh.nab.common.util.ExceptionUtils.getOrThrow;
import ru.hh.nab.kafka.KafkaTestConfig;

@SpringBootTest(classes = KafkaTestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public abstract class KafkaConsumerTestBase {

  @Inject
  protected TestKafkaWithJsonMessages testKafka;

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
        .build();
    consumer.start();
    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertEquals(5, consumer.getAssignedPartitions().size()));
    return consumer;
  }

  public void addPartitions(String topic, int finalPartitionsCount) throws InterruptedException, ExecutionException {
    AdminClient adminClient = testKafka.getAdminClient();
    adminClient.createPartitions(Map.of(topic, NewPartitions.increaseTo(finalPartitionsCount))).all().get();
    waitUntil(() -> assertEquals(
            finalPartitionsCount,
            getOrThrow(() -> adminClient.describeTopics(List.of(topic)).topicNameValues().get(topic).get().partitions().size())
        )
    );
  }

  protected void waitUntil(Runnable assertion) throws InterruptedException {
    await().atMost(10, TimeUnit.SECONDS).untilAsserted(assertion::run);
  }

  protected long countMessagesInTopic(String topic) {
    try (AdminClient adminClient = testKafka.getAdminClient()) {
      List<TopicPartitionInfo> partitions = adminClient.describeTopics(List.of(topic)).topicNameValues().get(topic).get().partitions();
      Map<TopicPartition, OffsetSpec> topicPartitionOffsetSpecs = partitions
          .stream()
          .map(TopicPartitionInfo::partition)
          .collect(toMap(partition -> new TopicPartition(topic, partition), partition -> OffsetSpec.latest()));
      Collection<ListOffsetsResult.ListOffsetsResultInfo> offsets = adminClient.listOffsets(topicPartitionOffsetSpecs).all().get().values();
      return offsets.stream().map(ListOffsetsResult.ListOffsetsResultInfo::offset).reduce(Long::sum).orElse(0L);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }
}
