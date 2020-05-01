package ru.hh.nab.kafka.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import static org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG;
import org.apache.kafka.clients.producer.ProducerConfig;
import ru.hh.nab.common.properties.FileSettings;

public class ConfigProvider {
  static final String COMMON_CONFIG_TEMPLATE = "%s.common";
  static final String DEFAULT_CONSUMER_CONFIG_TEMPLATE = "%s.consumer.default";
  static final String TOPIC_CONSUMER_CONFIG_TEMPLATE = "%s.consumer.topic.%s.default";
  static final String PRODUCER_CONFIG_TEMPLATE = "%s.producer.%s";
  static final String NAB_SETTING = "nab_setting";
  public static final String DEFAULT_PRODUCER_NAME = "default";

  public static final String BACKOFF_INITIAL_INTERVAL_NAME = "backoff.initial.interval";
  public static final long DEFAULT_BACKOFF_INITIAL_INTERVAL = 1000L;
  public static final String BACKOFF_MAX_INTERVAL_NAME = "backoff.max.interval";
  public static final long DEFAULT_BACKOFF_MAX_INTERVAL = 60000L;
  public static final String BACKOFF_MULTIPLIER_NAME = "backoff.multiplier";
  public static final double DEFAULT_BACKOFF_MULTIPLIER = 1.5;

  public static final String POOL_TIMEOUT = "pool.timeout.ms";
  public static final long DEFAULT_POOL_TIMEOUT = 5000L;

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
    consumeConfig.putAll(getAllConsumerConfigs(topicName));
    removeNabProperties(consumeConfig);
    return consumeConfig;
  }

  public FileSettings getNabConsumerSettings(String topicName) {
    Map<String, Object> allConsumerConfigs = getAllConsumerConfigs(topicName);

    Properties nabProperties = new Properties();
    nabProperties.putAll(allConsumerConfigs);
    removeNonNabProperties(nabProperties);
    FileSettings nabConsumerSettings = new FileSettings(nabProperties).getSubSettings(NAB_SETTING);

    Properties allProperties = new Properties();
    nabProperties.putAll(allConsumerConfigs);
    FileSettings allConsumerSettings = new FileSettings(allProperties);

    checkConfig(nabConsumerSettings, allConsumerSettings);
    return nabConsumerSettings;
  }

  private Map<String, Object> getAllConsumerConfigs(String topicName) {
    Map<String, Object> consumerConfig = new HashMap<>();
    consumerConfig.putAll(getCommonProperties());
    consumerConfig.putAll(getDefaultConsumerProperties());
    consumerConfig.putAll(getTopicOverriddenConsumerProperties(topicName));
    return consumerConfig;
  }

  private void checkConfig(FileSettings nabConsumerSettings, FileSettings allConsumerSettings) {
    long maxPollMs = allConsumerSettings.getLong(MAX_POLL_INTERVAL_MS_CONFIG, 300000L);
    long backoffMaxInterval = nabConsumerSettings.getLong(BACKOFF_MAX_INTERVAL_NAME, DEFAULT_BACKOFF_MAX_INTERVAL);
    if (backoffMaxInterval > maxPollMs) {
      throw new IllegalArgumentException(
          String.format("'%s' should not be larger then '%s'", BACKOFF_MAX_INTERVAL_NAME, MAX_POLL_INTERVAL_MS_CONFIG)
      );
    }
  }

  private Map<String, Object> getDefaultConsumerProperties() {
    return getConfigAsMap(String.format(DEFAULT_CONSUMER_CONFIG_TEMPLATE, kafkaClusterName));
  }

  private Map<String, Object> getTopicOverriddenConsumerProperties(String topicName) {
    return getConfigAsMap(String.format(TOPIC_CONSUMER_CONFIG_TEMPLATE, kafkaClusterName, topicName));
  }

  public Map<String, Object> getDefaultProducerConfig() {
    return getProducerConfig(DEFAULT_PRODUCER_NAME);
  }

  public Map<String, Object> getProducerConfig(String producerName) {
    Map<String, Object> producerConfig = new HashMap<>();
    producerConfig.put(ProducerConfig.CLIENT_ID_CONFIG, serviceName);
    producerConfig.putAll(getCommonProperties());
    producerConfig.putAll(getDefaultProducerProperties(producerName));
    return producerConfig;
  }

  private Map<String, Object> getDefaultProducerProperties(String producerName) {
    return getConfigAsMap(String.format(PRODUCER_CONFIG_TEMPLATE, kafkaClusterName, producerName));
  }

  private Map<String, Object> getCommonProperties() {
    return getConfigAsMap(String.format(COMMON_CONFIG_TEMPLATE, kafkaClusterName));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getConfigAsMap(String prefix) {
    return new HashMap<>((Map) fileSettings.getSubProperties(prefix));
  }

  private void removeNabProperties(Map<String, Object> config) {
    config.keySet().removeIf(key -> key.contains(NAB_SETTING));
  }

  private void removeNonNabProperties(Properties allProperties) {
    allProperties.keySet().removeIf(key -> !((String) key).contains(NAB_SETTING));
  }
}
