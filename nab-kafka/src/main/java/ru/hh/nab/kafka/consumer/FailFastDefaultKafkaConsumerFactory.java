package ru.hh.nab.kafka.consumer;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

class FailFastDefaultKafkaConsumerFactory<K, V> extends DefaultKafkaConsumerFactory<K, V> {

  private final String topicName;

  public FailFastDefaultKafkaConsumerFactory(String topicName,
                                             Map<String, Object> configs,
                                             Deserializer<K> keyDeserializer,
                                             Deserializer<V> valueDeserializer) {
    super(configs, keyDeserializer, valueDeserializer);
    this.topicName = topicName;
  }

  public FailFastDefaultKafkaConsumerFactory(
      String topicName,
      Map<String, Object> configs,
      Deserializer<K> keyDeserializer,
      Deserializer<V> valueDeserializer,
      Supplier<String> bootstrapSupplier
  ) {
    this(topicName, configs, keyDeserializer, valueDeserializer);
    this.setBootstrapServersSupplier(bootstrapSupplier);
  }

  @Override
  protected Consumer<K, V> createKafkaConsumer(String groupId, String clientIdPrefix, String clientIdSuffixArg, Properties properties) {
    Consumer<K, V> kafkaConsumer = super.createKafkaConsumer(groupId, clientIdPrefix, clientIdSuffixArg, properties);
    List<PartitionInfo> partitions = kafkaConsumer.partitionsFor(topicName); // fail if user is not authorized to access topic
    if (partitions == null || partitions.isEmpty()) {
      // fail if topic does not exist
      throw new IllegalStateException(String.format("Failed to find any partition for topic %s", topicName));
    }
    return kafkaConsumer;
  }
}
