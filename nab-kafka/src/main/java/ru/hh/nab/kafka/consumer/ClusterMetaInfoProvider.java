package ru.hh.nab.kafka.consumer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.PartitionInfo;
import org.springframework.kafka.core.ConsumerFactory;

public class ClusterMetaInfoProvider {

  private final Map<String, ConsumerFactory<String, String>> springConsumerFactoryCache = new ConcurrentHashMap<>();

  private final DefaultConsumerFactory defaultConsumerFactory;

  public ClusterMetaInfoProvider(DefaultConsumerFactory defaultConsumerFactory) {
    this.defaultConsumerFactory = defaultConsumerFactory;
  }

  public List<PartitionInfo> getPartitionsInfo(String topicName) {
    ConsumerFactory<String, String> springConsumerFactory = springConsumerFactoryCache.computeIfAbsent(
        topicName,
        k -> defaultConsumerFactory.getSpringConsumerFactory(topicName, String.class)
    );
    try (Consumer<String, String> consumer = springConsumerFactory.createConsumer()) {
      return consumer.partitionsFor(topicName);
    }
  }

}
