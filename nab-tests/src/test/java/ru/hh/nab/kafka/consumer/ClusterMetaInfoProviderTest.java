package ru.hh.nab.kafka.consumer;

import java.util.concurrent.ExecutionException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class ClusterMetaInfoProviderTest extends KafkaConsumerTestbase {


  @Test
  public void testGetPartitionsCountByClusterMetaInfoProvider() throws InterruptedException, ExecutionException {
    DefaultConsumerFactory defaultConsumerFactory = (DefaultConsumerFactory) consumerFactory;
    ClusterMetaInfoProvider clusterMetaInfoProvider = new ClusterMetaInfoProvider(defaultConsumerFactory);

    assertEquals(5, clusterMetaInfoProvider.getPartitionsInfo(topicName).size());

    addPartitions(topicName, 7);

    waitUntil(() -> assertEquals(7, clusterMetaInfoProvider.getPartitionsInfo(topicName).size()));
  }

}
