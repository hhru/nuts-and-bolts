package ru.hh.nab.kafka.consumer;

import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

class FailFastDefaultKafkaConsumerFactory<K, V> extends DefaultKafkaConsumerFactory<K, V> {

  private final String topicName;


  public FailFastDefaultKafkaConsumerFactory(
      String topicName,
      Map<String, Object> configs,
      Deserializer<K> keyDeserializer,
      Deserializer<V> valueDeserializer,
      Supplier<String> bootstrapServersSupplier
  ) {
    super(configs, keyDeserializer, valueDeserializer);
    this.topicName = topicName;
    this.setBootstrapServersSupplier(bootstrapServersSupplier);
  }

  @Override
  protected Consumer<K, V> createKafkaConsumer(String groupId, String clientIdPrefix, String clientIdSuffixArg, Properties properties) {
    Consumer<K, V> kafkaConsumer = super.createKafkaConsumer(groupId, clientIdPrefix, clientIdSuffixArg, properties);
    kafkaConsumer.partitionsFor(topicName); // fail if user is not authorized to access topic
    return kafkaConsumer;
  }
}
