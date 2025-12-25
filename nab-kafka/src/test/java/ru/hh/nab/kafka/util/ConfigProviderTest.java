package ru.hh.nab.kafka.util;

import com.timgroup.statsd.NoOpStatsDClient;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import ru.hh.nab.common.executor.ScheduledExecutor;
import static ru.hh.nab.kafka.util.ConfigProvider.COMMON_CONFIG_TEMPLATE;
import static ru.hh.nab.kafka.util.ConfigProvider.DEFAULT_CONSUMER_CONFIG_TEMPLATE;
import static ru.hh.nab.kafka.util.ConfigProvider.DEFAULT_PRODUCER_NAME;
import static ru.hh.nab.kafka.util.ConfigProvider.PRODUCER_CONFIG_TEMPLATE;
import static ru.hh.nab.kafka.util.ConfigProvider.TOPIC_CONSUMER_CONFIG_TEMPLATE;
import ru.hh.nab.metrics.StatsDSender;

public class ConfigProviderTest {

  private static final String SERVICE_NAME = "testService";
  private static final String KAFKA_CLUSTER_NAME = "kafka";
  private static final String CONSUMER_TEST_KEY = ConsumerConfig.CLIENT_RACK_CONFIG;
  private static final String PRODUCER_TEST_KEY = ProducerConfig.TRANSACTIONAL_ID_CONFIG;

  private static String generateSettingKey(String template, String testKey) {
    return String.format(template, KAFKA_CLUSTER_NAME) + "." + testKey;
  }

  private static String generateSettingKey(String template, String topicName, String testKey) {
    return String.format(template, KAFKA_CLUSTER_NAME, topicName) + "." + testKey;
  }

  private static String generateProducerSettingKey(String testKey) {
    return generateProducerSettingKey(DEFAULT_PRODUCER_NAME, testKey);
  }

  private static String generateProducerSettingKey(String producerName, String testKey) {
    return String.format(PRODUCER_CONFIG_TEMPLATE, KAFKA_CLUSTER_NAME, producerName) + "." + testKey;
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
  public void shouldReturnCommonSettings() {
    String testValue = "value";
    Properties settings = createSettings(Map.of(
        generateSettingKey(COMMON_CONFIG_TEMPLATE, CONSUMER_TEST_KEY), testValue
    ));

    var result = createConfigProvider(settings).getConsumerConfig("ignored");

    assertEquals(testValue, result.get(CONSUMER_TEST_KEY));
  }

  @Test
  public void shouldContainServiceNameSetting() {
    var result = createConfigProvider(createSettings(Map.of())).getConsumerConfig("ignored");

    assertEquals(SERVICE_NAME, result.get(ConsumerConfig.CLIENT_ID_CONFIG));
  }

  @Test
  public void shouldReturnConsumerDefaultSetting() {
    String testValue = "value";
    Properties settings = createSettings(Map.of(
        generateSettingKey(DEFAULT_CONSUMER_CONFIG_TEMPLATE, CONSUMER_TEST_KEY), testValue
    ));

    var result = createConfigProvider(settings).getConsumerConfig("ignored");

    assertEquals(testValue, result.get(CONSUMER_TEST_KEY));
  }

  @Test
  public void shouldReturnOverriddenConsumerSettingForSpecificTopic() {
    String defaultValue = "value";
    String overriddenValue = "newValue";
    String topicName = "topic";
    Properties settings = createSettings(Map.of(
        generateSettingKey(DEFAULT_CONSUMER_CONFIG_TEMPLATE, CONSUMER_TEST_KEY), defaultValue,
        generateSettingKey(TOPIC_CONSUMER_CONFIG_TEMPLATE, topicName, CONSUMER_TEST_KEY), overriddenValue
    ));

    ConfigProvider configProvider = createConfigProvider(settings);

    var result = configProvider.getConsumerConfig(topicName);
    assertEquals(overriddenValue, result.get(CONSUMER_TEST_KEY));

    result = configProvider.getConsumerConfig("ignored");
    assertEquals(defaultValue, result.get(CONSUMER_TEST_KEY));
  }

  @Test
  public void shouldFailOnUnusedOverriddenConsumerSettingForSpecificTopic() {
    String testKey = ConsumerConfig.CLIENT_RACK_CONFIG;
    String defaultValue = "value";
    String overriddenValue = "newValue";
    String invalidOverriddenValue = "invalidValue";
    String topicName = "topic";
    Properties settings = createSettings(Map.of(
        generateSettingKey(DEFAULT_CONSUMER_CONFIG_TEMPLATE, testKey), defaultValue,
        generateSettingKey(TOPIC_CONSUMER_CONFIG_TEMPLATE, topicName, testKey), overriddenValue,
        generateSettingKey("%s.consumer.topic.%s", topicName, testKey), invalidOverriddenValue
    ));

    ConfigProvider configProvider = createConfigProvider(settings);

    var exception = assertThrows(IllegalArgumentException.class, () -> configProvider.getConsumerConfig(topicName));
    assertEquals("Unused property found: 'kafka.consumer.topic.topic.client.rack'", exception.getMessage());
  }

  @Test
  public void shouldFailOnUnsupportedConsumerSetting() {
    String defaultValue = "value";
    String topicName = "topic";
    Properties settings = createSettings(Map.of(
        generateSettingKey(DEFAULT_CONSUMER_CONFIG_TEMPLATE, "key1"), defaultValue,
        generateSettingKey(DEFAULT_CONSUMER_CONFIG_TEMPLATE, "key2"), defaultValue,
        generateSettingKey(DEFAULT_CONSUMER_CONFIG_TEMPLATE, "key3"), defaultValue
    ));

    ConfigProvider configProvider = createConfigProvider(settings);

    var exception = assertThrows(IllegalArgumentException.class, () -> configProvider.getConsumerConfig(topicName));
    assertEquals("Unsupported kafka consumer properties found: 'key1', 'key2', 'key3'", exception.getMessage());
  }

  @Test
  public void shouldReturnDefaultProducerSetting() {
    String testValue = "value";
    Properties settings = createSettings(Map.of(
        generateProducerSettingKey(PRODUCER_TEST_KEY), testValue
    ));

    var result = createConfigProvider(settings).getDefaultProducerConfig();

    assertEquals(testValue, result.get(PRODUCER_TEST_KEY));
  }

  @Test
  public void shouldReturnDifferentProducerSettings() {
    String testKey1 = ProducerConfig.BATCH_SIZE_CONFIG;
    String testValue1 = "444";
    String testKey2 = ProducerConfig.MAX_REQUEST_SIZE_CONFIG;
    String testValue2 = "555";
    Properties settings = createSettings(Map.of(
        generateProducerSettingKey("1", testKey1), testValue1,
        generateProducerSettingKey("2", testKey2), testValue2
    ));

    var result = createConfigProvider(settings).getProducerConfig("1");
    assertEquals(testValue1, result.get(testKey1));

    result = createConfigProvider(settings).getProducerConfig("2");
    assertEquals(testValue2, result.get(testKey2));
  }

  @Test
  public void shouldFailOnUnsupportedProducerSetting() {
    String defaultValue = "value";
    Properties settings = createSettings(Map.of(
        generateProducerSettingKey("key1"), defaultValue,
        generateProducerSettingKey("key2"), defaultValue,
        generateProducerSettingKey("key3"), defaultValue
    ));

    ConfigProvider configProvider = createConfigProvider(settings);

    var exception = assertThrows(IllegalArgumentException.class, configProvider::getDefaultProducerConfig);
    assertEquals("Unsupported kafka producer properties found: 'key1', 'key2', 'key3'", exception.getMessage());
  }
}
