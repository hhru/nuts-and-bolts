package ru.hh.nab.kafka.consumer;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.common.PartitionInfo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class TopicPartitionsMonitoringTest extends KafkaConsumerTestBase {

  @Test
  public void testSubsceribeForPartitionsChanges() throws InterruptedException, ExecutionException {
    DefaultConsumerFactory defaultConsumerFactory = (DefaultConsumerFactory) consumerFactory;
    ClusterMetadataProvider clusterMetadataProvider = new ClusterMetadataProvider(defaultConsumerFactory);
    TopicPartitionsMonitoring topicPartitionsMonitoring = new TopicPartitionsMonitoring(clusterMetadataProvider);

    List<PartitionInfo> initialPartitions = clusterMetadataProvider.getPartitionsInfo(topicName);
    assertEquals(5, initialPartitions.size());

    CountDownLatch latch = new CountDownLatch(1);
    topicPartitionsMonitoring.subscribeOnPartitionsChange(topicName, Duration.ofSeconds(1), initialPartitions, (newPartitions) -> {
      if (newPartitions.size() == 7) {
        latch.countDown();
      }
    });

    addPartitions(topicName, 7);

    assertTrue(latch.await(5, TimeUnit.SECONDS));
  }

}
