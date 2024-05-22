package ru.hh.nab.kafka.consumer;

import java.util.concurrent.ExecutionException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class ClusterMetadataProviderTest extends KafkaConsumerTestbase {


  @Test
  public void testGetPartitionsCountByClusterMetadataProvider() throws InterruptedException, ExecutionException {
    DefaultConsumerFactory defaultConsumerFactory = (DefaultConsumerFactory) consumerFactory;
    ClusterMetadataProvider clusterMetadataProvider = new ClusterMetadataProvider(defaultConsumerFactory);

    assertEquals(5, clusterMetadataProvider.getPartitionsInfo(topicName).size());

    addPartitions(topicName, 7);

    waitUntil(() -> assertEquals(7, clusterMetadataProvider.getPartitionsInfo(topicName).size()));
  }

}
