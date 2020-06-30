package ru.hh.nab.kafka.util;

import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.kafka.util.ConfigProvider.COMMON_CONFIG_TEMPLATE;
import static ru.hh.nab.kafka.util.ConfigProvider.DEFAULT_CONSUMER_CONFIG_TEMPLATE;
import static ru.hh.nab.kafka.util.ConfigProvider.DEFAULT_PRODUCER_NAME;
import static ru.hh.nab.kafka.util.ConfigProvider.PRODUCER_CONFIG_TEMPLATE;
import static ru.hh.nab.kafka.util.ConfigProvider.TOPIC_CONSUMER_CONFIG_TEMPLATE;

public class ConfigProviderTest {

  private static final String SERVICE_NAME = "testService";
  private static final String KAFKA_CLUSTER_NAME = "kafka";

  @Test
  public void shouldReturnCommonSettings() {
    String testKey = "key";
    String testValue = "value";
    FileSettings fileSettings = createFileSettings(Map.of(
        generateSettingKey(COMMON_CONFIG_TEMPLATE, testKey), testValue
    ));

    var result = createConfigProvider(fileSettings).getConsumerConfig("ignored");

    assertEquals(testValue, result.get(testKey));
  }

  @Test
  public void shouldContainServiceNameSetting() {
    var result = createConfigProvider(createFileSettings(Map.of())).getConsumerConfig("ignored");

    assertEquals(SERVICE_NAME, result.get(ConsumerConfig.CLIENT_ID_CONFIG));
  }

  @Test
  public void shouldReturnConsumerDefaultSetting() {
    String testKey = "key";
    String testValue = "value";
    FileSettings fileSettings = createFileSettings(Map.of(
        generateSettingKey(DEFAULT_CONSUMER_CONFIG_TEMPLATE, testKey), testValue
    ));

    var result = createConfigProvider(fileSettings).getConsumerConfig("ignored");

    assertEquals(testValue, result.get(testKey));
  }

  @Test
  public void shouldReturnOverriddenConsumerSettingForSpecificTopic() {
    String testKey = "key";
    String defaultValue = "value";
    String overriddenValue = "newValue";
    String topicName = "topic";
    FileSettings fileSettings = createFileSettings(Map.of(
        generateSettingKey(DEFAULT_CONSUMER_CONFIG_TEMPLATE, testKey), defaultValue,
        generateSettingKey(TOPIC_CONSUMER_CONFIG_TEMPLATE, topicName, testKey), overriddenValue
    ));

    ConfigProvider configProvider = createConfigProvider(fileSettings);

    var result = configProvider.getConsumerConfig(topicName);
    assertEquals(overriddenValue, result.get(testKey));

    result = configProvider.getConsumerConfig("ignored");
    assertEquals(defaultValue, result.get(testKey));
  }

  @Test
  public void shouldFailOnUnusedOverriddenConsumerSettingForSpecificTopic() {
    String testKey = "key";
    String defaultValue = "value";
    String overriddenValue = "newValue";
    String invalidOverriddenValue = "invalidValue";
    String topicName = "topic";
    FileSettings fileSettings = createFileSettings(Map.of(
        generateSettingKey(DEFAULT_CONSUMER_CONFIG_TEMPLATE, testKey), defaultValue,
        generateSettingKey(TOPIC_CONSUMER_CONFIG_TEMPLATE, topicName, testKey), overriddenValue,
        generateSettingKey("%s.consumer.topic.%s", topicName, testKey), invalidOverriddenValue
    ));

    ConfigProvider configProvider = createConfigProvider(fileSettings);

    var exception = assertThrows(IllegalArgumentException.class, () -> configProvider.getConsumerConfig(topicName));
    assertEquals("Unused property found: 'kafka.consumer.topic.topic.key'", exception.getMessage());
  }

  @Test
  public void shouldReturnDefaultProducerSetting() {
    String testKey = "key";
    String testValue = "value";
    FileSettings fileSettings = createFileSettings(Map.of(
        generateProducerSettingKey(testKey), testValue
    ));

    var result = createConfigProvider(fileSettings).getDefaultProducerConfig();

    assertEquals(testValue, result.get(testKey));
  }

  @Test
  public void shouldReturnDifferentProducerSettings() {
    String testKey1 = "key1";
    String testValue1 = "value1";
    String testKey2 = "key2";
    String testValue2 = "value2";
    FileSettings fileSettings = createFileSettings(Map.of(
        generateProducerSettingKey("1", testKey1), testValue1,
        generateProducerSettingKey("2", testKey2), testValue2
    ));

    var result = createConfigProvider(fileSettings).getProducerConfig("1");
    assertEquals(testValue1, result.get(testKey1));

    result = createConfigProvider(fileSettings).getProducerConfig("2");
    assertEquals(testValue2, result.get(testKey2));
  }

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

  private static ConfigProvider createConfigProvider(FileSettings fileSettings) {
    return new ConfigProvider(SERVICE_NAME, KAFKA_CLUSTER_NAME, fileSettings);
  }

  private static FileSettings createFileSettings(Map<String, Object> props) {
    Properties properties = new Properties();
    properties.putAll(props);
    return new FileSettings(properties);
  }
}
