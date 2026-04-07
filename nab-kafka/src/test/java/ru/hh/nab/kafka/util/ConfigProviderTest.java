package ru.hh.nab.kafka.util;

import com.timgroup.statsd.NoOpStatsDClient;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.record.CompressionType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import ru.hh.nab.common.executor.ScheduledExecutor;
import static ru.hh.nab.kafka.util.ConfigProvider.CLUSTER_CONSUMER_CONFIG_TEMPLATE;
import static ru.hh.nab.kafka.util.ConfigProvider.COMMON_CONFIG_TEMPLATE;
import static ru.hh.nab.kafka.util.ConfigProvider.DEFAULT_PRODUCER_NAME;
import static ru.hh.nab.kafka.util.ConfigProvider.DEFAULT_PSEUDO_CLUSTER;
import static ru.hh.nab.kafka.util.ConfigProvider.PRODUCER_CONFIG_TEMPLATE;
import static ru.hh.nab.kafka.util.ConfigProvider.TOPIC_CONSUMER_CONFIG_TEMPLATE;
import ru.hh.nab.metrics.StatsDSender;

public class ConfigProviderTest {

  private static final String SERVICE_NAME = "testService";
  private static final String KAFKA_CLUSTER_NAME = "kafka.test";
  private static final String COMMON_TEST_KEY = CommonClientConfigs.RECONNECT_BACKOFF_MS_CONFIG;
  private static final String CONSUMER_TEST_KEY = ConsumerConfig.CLIENT_RACK_CONFIG;

  private static String generateSettingKey(String template, String clusterName, String testKey) {
    return String.format(template, clusterName) + "." + testKey;
  }

  private static String generateSettingKey(String template, String clusterName, String topicName, String testKey) {
    return String.format(template, clusterName, topicName) + "." + testKey;
  }

  private static String generateProducerSettingKey(String clusterName, String producerName, String testKey) {
    return String.format(PRODUCER_CONFIG_TEMPLATE, clusterName, producerName) + "." + testKey;
  }

  private static ConfigProvider createConfigProvider(Properties settings) {
    StatsDSender statsDSender = new StatsDSender(new NoOpStatsDClient(), new ScheduledExecutor(), StatsDSender.DEFAULT_SEND_INTERVAL_SECONDS);
    return new ConfigProvider(SERVICE_NAME, KAFKA_CLUSTER_NAME, settings, statsDSender);
  }

  private static Properties createSettings(Map<String, Object> props) {
    Properties properties = new Properties();
    properties.putAll(props);
    return properties;
  }

  @Test
  public void shouldFailOnCommonSettingsForAllClusters() {
    String invalidAllClustersValue = "allClustersValue";
    Properties settings = createSettings(Map.of(
        generateSettingKey(COMMON_CONFIG_TEMPLATE, DEFAULT_PSEUDO_CLUSTER, COMMON_TEST_KEY), invalidAllClustersValue
    ));

    ConfigProvider configProvider = createConfigProvider(settings);

    var exception = assertThrows(IllegalArgumentException.class, () -> configProvider.getConsumerConfig("ignored"));
    assertEquals("Unused property found: 'kafka.default.common.reconnect.backoff.ms'", exception.getMessage());

    exception = assertThrows(IllegalArgumentException.class, () -> configProvider.getProducerConfig("ignored"));
    assertEquals("Unused property found: 'kafka.default.common.reconnect.backoff.ms'", exception.getMessage());
  }

  @Test
  public void shouldReturnCommonSettings() {
    String testValue = "value";
    Properties settings = createSettings(Map.of(
        generateSettingKey(COMMON_CONFIG_TEMPLATE, KAFKA_CLUSTER_NAME, COMMON_TEST_KEY), testValue
    ));

    ConfigProvider configProvider = createConfigProvider(settings);

    assertEquals(testValue, configProvider.getConsumerConfig("ignored").get(COMMON_TEST_KEY));
    assertEquals(testValue, configProvider.getProducerConfig("ignored").get(COMMON_TEST_KEY));
  }

  @Test
  public void shouldContainServiceNameSetting() {
    var result = createConfigProvider(createSettings(Map.of())).getConsumerConfig("ignored");

    assertEquals(SERVICE_NAME, result.get(ConsumerConfig.CLIENT_ID_CONFIG));
  }

  @Test
  public void shouldReturnConsumerSettingForAllClusters() {
    String allClustersValue = "allClustersValue";
    Properties settings = createSettings(Map.of(
        generateSettingKey(CLUSTER_CONSUMER_CONFIG_TEMPLATE, DEFAULT_PSEUDO_CLUSTER, CONSUMER_TEST_KEY), allClustersValue
    ));

    var result = createConfigProvider(settings).getConsumerConfig("ignored");

    assertEquals(allClustersValue, result.get(CONSUMER_TEST_KEY));
  }

  @Test
  public void shouldReturnOverriddenConsumerSettingForSpecificCluster() {
    String allClustersValue = "allClustersValue";
    String clusterValue = "clusterValue";
    Properties settings = createSettings(Map.of(
        generateSettingKey(CLUSTER_CONSUMER_CONFIG_TEMPLATE, DEFAULT_PSEUDO_CLUSTER, CONSUMER_TEST_KEY), allClustersValue,
        generateSettingKey(CLUSTER_CONSUMER_CONFIG_TEMPLATE, KAFKA_CLUSTER_NAME, CONSUMER_TEST_KEY), clusterValue
    ));

    ConfigProvider configProvider = createConfigProvider(settings);

    var result = configProvider.getConsumerConfig("ignored");
    assertEquals(clusterValue, result.get(CONSUMER_TEST_KEY));
  }

  @Test
  public void shouldFailOnUnusedConsumerSettingForSpecificCluster() {
    String invalidClusterValue = "invalidClusterValue";
    Properties settings = createSettings(Map.of(
        generateSettingKey("%s.consumer", KAFKA_CLUSTER_NAME, CONSUMER_TEST_KEY), invalidClusterValue
    ));

    ConfigProvider configProvider = createConfigProvider(settings);

    var exception = assertThrows(IllegalArgumentException.class, () -> configProvider.getConsumerConfig("ignored"));
    assertEquals("Unused property found: 'kafka.test.consumer.client.rack'", exception.getMessage());
  }

  @Test
  public void shouldFailOnSpecificTopicSettingForAllClusters() {
    String invalidAllClustersValue = "allClustersValue";
    String topicName = "topic";
    Properties settings = createSettings(Map.of(
        generateSettingKey(TOPIC_CONSUMER_CONFIG_TEMPLATE, DEFAULT_PSEUDO_CLUSTER, topicName, CONSUMER_TEST_KEY), invalidAllClustersValue
    ));

    ConfigProvider configProvider = createConfigProvider(settings);

    var exception = assertThrows(IllegalArgumentException.class, () -> configProvider.getConsumerConfig("ignored"));
    assertEquals("Unused property found: 'kafka.default.consumer.topic.topic.default.client.rack'", exception.getMessage());
  }

  @Test
  public void shouldReturnOverriddenConsumerSettingForSpecificTopic() {
    String allClustersValue = "allClustersValue";
    String clusterValue = "clusterValue";
    String topicValue = "topicValue";
    String topicName = "topic";
    Properties settings = createSettings(Map.of(
        generateSettingKey(CLUSTER_CONSUMER_CONFIG_TEMPLATE, DEFAULT_PSEUDO_CLUSTER, CONSUMER_TEST_KEY), allClustersValue,
        generateSettingKey(CLUSTER_CONSUMER_CONFIG_TEMPLATE, KAFKA_CLUSTER_NAME, CONSUMER_TEST_KEY), clusterValue,
        generateSettingKey(TOPIC_CONSUMER_CONFIG_TEMPLATE, KAFKA_CLUSTER_NAME, topicName, CONSUMER_TEST_KEY), topicValue
    ));

    ConfigProvider configProvider = createConfigProvider(settings);

    var result = configProvider.getConsumerConfig(topicName);
    assertEquals(topicValue, result.get(CONSUMER_TEST_KEY));

    result = configProvider.getConsumerConfig("ignored");
    assertEquals(clusterValue, result.get(CONSUMER_TEST_KEY));
  }

  @Test
  public void shouldFailOnUnusedConsumerSettingForSpecificTopic() {
    String testKey = ConsumerConfig.CLIENT_RACK_CONFIG;
    String clusterValue = "clusterValue";
    String validTopicValue = "validTopicValue";
    String invalidTopicValue = "invalidTopicValue";
    String topicName = "topic";
    Properties settings = createSettings(Map.of(
        generateSettingKey(CLUSTER_CONSUMER_CONFIG_TEMPLATE, KAFKA_CLUSTER_NAME, testKey), clusterValue,
        generateSettingKey(TOPIC_CONSUMER_CONFIG_TEMPLATE, KAFKA_CLUSTER_NAME, topicName, testKey), validTopicValue,
        generateSettingKey("%s.consumer.topic.%s", KAFKA_CLUSTER_NAME, topicName, testKey), invalidTopicValue
    ));

    ConfigProvider configProvider = createConfigProvider(settings);

    var exception = assertThrows(IllegalArgumentException.class, () -> configProvider.getConsumerConfig(topicName));
    assertEquals("Unused property found: 'kafka.test.consumer.topic.topic.client.rack'", exception.getMessage());
  }

  @Test
  public void shouldFailOnUnsupportedConsumerSetting() {
    String defaultValue = "value";
    String topicName = "topic";
    Properties settings = createSettings(Map.of(
        generateSettingKey(CLUSTER_CONSUMER_CONFIG_TEMPLATE, KAFKA_CLUSTER_NAME, "key1"), defaultValue,
        generateSettingKey(CLUSTER_CONSUMER_CONFIG_TEMPLATE, KAFKA_CLUSTER_NAME, "key2"), defaultValue,
        generateSettingKey(CLUSTER_CONSUMER_CONFIG_TEMPLATE, KAFKA_CLUSTER_NAME, "key3"), defaultValue
    ));

    ConfigProvider configProvider = createConfigProvider(settings);

    var exception = assertThrows(IllegalArgumentException.class, () -> configProvider.getConsumerConfig(topicName));
    assertEquals("Unsupported kafka consumer properties found: 'key1', 'key2', 'key3'", exception.getMessage());
  }

  @Test
  public void testDefaultProducerConfigUsed() {
    // Create default settings for producers
    String lingerDefaultValue = "100";
    String batchSizeDefaultValue = "262144";
    String compressionDefaultValue = CompressionType.LZ4.name();
    Properties settings = createSettings(Map.of(
        generateProducerSettingKey(DEFAULT_PSEUDO_CLUSTER, DEFAULT_PRODUCER_NAME, ProducerConfig.LINGER_MS_CONFIG), lingerDefaultValue,
        generateProducerSettingKey(DEFAULT_PSEUDO_CLUSTER, DEFAULT_PRODUCER_NAME, ProducerConfig.BATCH_SIZE_CONFIG), batchSizeDefaultValue,
        generateProducerSettingKey(DEFAULT_PSEUDO_CLUSTER, DEFAULT_PRODUCER_NAME, ProducerConfig.COMPRESSION_TYPE_CONFIG), compressionDefaultValue
    ));
    ConfigProvider configProvider = createConfigProvider(settings);

    // Ensure that default settings preserved across all producers
    Map<String, Object> producerConfig1 = configProvider.getProducerConfig("1");
    Map<String, Object> producerConfig2 = configProvider.getProducerConfig("2");
    assertEquals(producerConfig1, producerConfig2, "Default settings are not preserved across all producers");
    assertEquals(lingerDefaultValue, producerConfig1.get(ProducerConfig.LINGER_MS_CONFIG));
    assertEquals(batchSizeDefaultValue, producerConfig1.get(ProducerConfig.BATCH_SIZE_CONFIG));
    assertEquals(compressionDefaultValue, producerConfig1.get(ProducerConfig.COMPRESSION_TYPE_CONFIG));
  }

  @Test
  public void testDefaultProducerConfigOverride() {
    String producerName = "1";

    // Create default settings for producers
    String lingerOverride = "2";
    String batchOverride = "2";
    String compressionOverride = CompressionType.GZIP.name();
    String lingerDefault = "100";
    String batchDefault = "262144";
    String compressionDefault = CompressionType.LZ4.name();
    String lingerAllClustersDefault = "200";
    String batchAllClustersDefault = "30000";
    String compressionAllClustersDefault = CompressionType.ZSTD.name();
    Properties settings = createSettings(Map.of(
        generateProducerSettingKey(DEFAULT_PSEUDO_CLUSTER, DEFAULT_PRODUCER_NAME, ProducerConfig.LINGER_MS_CONFIG), lingerAllClustersDefault,
        generateProducerSettingKey(DEFAULT_PSEUDO_CLUSTER, DEFAULT_PRODUCER_NAME, ProducerConfig.BATCH_SIZE_CONFIG), batchAllClustersDefault,

        generateProducerSettingKey(DEFAULT_PSEUDO_CLUSTER, DEFAULT_PRODUCER_NAME, ProducerConfig.COMPRESSION_TYPE_CONFIG),
        compressionAllClustersDefault,

        generateProducerSettingKey(KAFKA_CLUSTER_NAME, DEFAULT_PRODUCER_NAME, ProducerConfig.LINGER_MS_CONFIG), lingerDefault,
        generateProducerSettingKey(KAFKA_CLUSTER_NAME, DEFAULT_PRODUCER_NAME, ProducerConfig.BATCH_SIZE_CONFIG), batchDefault,
        generateProducerSettingKey(KAFKA_CLUSTER_NAME, DEFAULT_PRODUCER_NAME, ProducerConfig.COMPRESSION_TYPE_CONFIG), compressionDefault,
        generateProducerSettingKey(KAFKA_CLUSTER_NAME, producerName, ProducerConfig.LINGER_MS_CONFIG), lingerOverride,
        generateProducerSettingKey(KAFKA_CLUSTER_NAME, producerName, ProducerConfig.BATCH_SIZE_CONFIG), batchOverride,
        generateProducerSettingKey(KAFKA_CLUSTER_NAME, producerName, ProducerConfig.COMPRESSION_TYPE_CONFIG), compressionOverride
    ));
    ConfigProvider configProvider = createConfigProvider(settings);

    // Ensure that default settings are not preserved
    Map<String, Object> overrideConfig = configProvider.getProducerConfig(producerName);
    Map<String, Object> defaultConfig = configProvider.getDefaultProducerConfig();
    assertNotEquals(overrideConfig, defaultConfig, "Default settings should not be preserved for first producer");
    assertEquals(lingerOverride, overrideConfig.get(ProducerConfig.LINGER_MS_CONFIG));
    assertEquals(batchOverride, overrideConfig.get(ProducerConfig.BATCH_SIZE_CONFIG));
    assertEquals(compressionOverride, overrideConfig.get(ProducerConfig.COMPRESSION_TYPE_CONFIG));
    assertEquals(lingerDefault, defaultConfig.get(ProducerConfig.LINGER_MS_CONFIG));
    assertEquals(batchDefault, defaultConfig.get(ProducerConfig.BATCH_SIZE_CONFIG));
    assertEquals(compressionDefault, defaultConfig.get(ProducerConfig.COMPRESSION_TYPE_CONFIG));
  }

  @Test
  public void testMultipleProducerConfigsUsed() {
    String testKey1 = ProducerConfig.BATCH_SIZE_CONFIG;
    String testValue1 = "444";
    String testKey2 = ProducerConfig.MAX_REQUEST_SIZE_CONFIG;
    String testValue2 = "555";
    Properties settings = createSettings(Map.of(
        generateProducerSettingKey(KAFKA_CLUSTER_NAME, "1", testKey1), testValue1,
        generateProducerSettingKey(KAFKA_CLUSTER_NAME, "2", testKey2), testValue2
    ));

    var result = createConfigProvider(settings).getProducerConfig("1");
    assertEquals(testValue1, result.get(testKey1));

    result = createConfigProvider(settings).getProducerConfig("2");
    assertEquals(testValue2, result.get(testKey2));
  }

  @Test
  public void testFailOnUnsupportedProducerSetting() {
    String defaultValue = "value";
    Properties settings = createSettings(Map.of(
        generateProducerSettingKey(KAFKA_CLUSTER_NAME, DEFAULT_PRODUCER_NAME, "key1"), defaultValue,
        generateProducerSettingKey(KAFKA_CLUSTER_NAME, DEFAULT_PRODUCER_NAME, "key2"), defaultValue,
        generateProducerSettingKey(KAFKA_CLUSTER_NAME, DEFAULT_PRODUCER_NAME, "key3"), defaultValue
    ));

    ConfigProvider configProvider = createConfigProvider(settings);

    var exception = assertThrows(IllegalArgumentException.class, configProvider::getDefaultProducerConfig);
    assertEquals("Unsupported kafka producer properties found: 'key1', 'key2', 'key3'", exception.getMessage());
  }
}
