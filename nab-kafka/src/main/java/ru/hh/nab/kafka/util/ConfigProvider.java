package ru.hh.nab.kafka.util;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import ru.hh.nab.common.properties.FileSettings;
import java.util.HashMap;
import java.util.Map;

public class ConfigProvider {

  static final String COMMON_CONFIG_TEMPLATE = "%s.common";
  static final String DEFAULT_CONSUMER_CONFIG_TEMPLATE = "%s.consumer.default";
  static final String TOPIC_CONSUMER_CONFIG_TEMPLATE = "%s.consumer.topic.%s.default";
  static final String DEFAULT_PRODUCER_CONFIG_TEMPLATE = "%s.producer.default";
  static final String TOPIC_PRODUCER_CONFIG_TEMPLATE = "%s.producer.topic.%s.default";

  private final String serviceName;
  private final String kafkaClusterName;
  private final FileSettings fileSettings;

  public ConfigProvider(String serviceName, String kafkaClusterName, FileSettings fileSettings) {
    this.serviceName = serviceName;
    this.kafkaClusterName = kafkaClusterName;
    this.fileSettings = fileSettings;
  }

  public String getServiceName() {
    return serviceName;
  }

  public Map<String, Object> getConsumerConfig(String topicName) {
    Map<String, Object> consumeConfig = new HashMap<>();
    consumeConfig.put(ConsumerConfig.CLIENT_ID_CONFIG, serviceName);
    consumeConfig.putAll(getCommonProperties());
    consumeConfig.putAll(getDefaultConsumerProperties());
    consumeConfig.putAll(getTopicOverriddenConsumerProperties(topicName));
    return consumeConfig;
  }

  private Map<String, Object> getDefaultConsumerProperties() {
    return getConfigAsMap(String.format(DEFAULT_CONSUMER_CONFIG_TEMPLATE, kafkaClusterName));
  }

  private Map<String, Object> getTopicOverriddenConsumerProperties(String topicName) {
    return getConfigAsMap(String.format(TOPIC_CONSUMER_CONFIG_TEMPLATE, kafkaClusterName, topicName));
  }

  public Map<String, Object> getProducerConfig(String topicName) {
    Map<String, Object> producerConfig = new HashMap<>();
    producerConfig.put(ProducerConfig.CLIENT_ID_CONFIG, serviceName);
    producerConfig.putAll(getCommonProperties());
    producerConfig.putAll(getDefaultProducerProperties());
    producerConfig.putAll(getTopicOverriddenProducerProperties(topicName));
    return producerConfig;
  }

  private Map<String, Object> getDefaultProducerProperties() {
    return getConfigAsMap(String.format(DEFAULT_PRODUCER_CONFIG_TEMPLATE, kafkaClusterName));
  }

  private Map<String, Object> getTopicOverriddenProducerProperties(String topicName) {
    return getConfigAsMap(String.format(TOPIC_PRODUCER_CONFIG_TEMPLATE, kafkaClusterName, topicName));
  }

  private Map<String, Object> getCommonProperties() {
    return getConfigAsMap(String.format(COMMON_CONFIG_TEMPLATE, kafkaClusterName));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getConfigAsMap(String prefix) {
    return new HashMap<>((Map) fileSettings.getSubProperties(prefix));
  }
}
