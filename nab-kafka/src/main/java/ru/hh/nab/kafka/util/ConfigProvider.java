package ru.hh.nab.kafka.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import static org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG;
import org.apache.kafka.clients.producer.ProducerConfig;
import static org.springframework.kafka.listener.ContainerProperties.DEFAULT_SHUTDOWN_TIMEOUT;
import ru.hh.nab.common.properties.PropertiesUtils;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import ru.hh.nab.kafka.monitoring.KafkaStatsDReporter;
import ru.hh.nab.metrics.StatsDSender;

public class ConfigProvider {
  public static final String NAB_SETTING = "nab_setting";
  public static final String DEFAULT_PRODUCER_NAME = "default";
  public static final String BACKOFF_INITIAL_INTERVAL_NAME = "backoff.initial.interval";
  public static final long DEFAULT_BACKOFF_INITIAL_INTERVAL = 1000L;
  public static final String BACKOFF_MAX_INTERVAL_NAME = "backoff.max.interval";
  public static final long DEFAULT_BACKOFF_MAX_INTERVAL = 60000L;
  public static final String BACKOFF_MULTIPLIER_NAME = "backoff.multiplier";
  public static final double DEFAULT_BACKOFF_MULTIPLIER = 1.5;
  public static final String POLL_TIMEOUT = "poll.timeout.ms";
  public static final long DEFAULT_POLL_TIMEOUT_MS = 5000L;
  public static final String SHUTDOWN_TIMEOUT_MS = "shutdown.timeout.ms";
  public static final long DEFAULT_SHUTDOWN_TIMEOUT_MS = DEFAULT_SHUTDOWN_TIMEOUT;
  public static final String AUTH_EXCEPTION_RETRY_INTERVAL = "auth.exception.retry.interval.ms";
  public static final long DEFAULT_AUTH_EXCEPTION_RETRY_INTERVAL_MS = 10000L;
  public static final String CONCURRENCY = "concurrency";
  static final String COMMON_CONFIG_TEMPLATE = "%s.common";
  static final String DEFAULT_CONSUMER_CONFIG_TEMPLATE = "%s.consumer.default";
  static final String TOPIC_CONSUMER_CONFIG_TEMPLATE = "%s.consumer.topic.%s.default";
  static final String TOPIC_CONSUMER_INVALID_CONFIG_REGEXP_TEMPLATE =
      "%s\\.consumer\\.topic\\.%s\\.(?!\\w*(?:default)).*";
  static final String PRODUCER_CONFIG_TEMPLATE = "%s.producer.%s";
  static final Predicate<Object> NAB_SETTING_PREDICATE = key -> ((String) key).startsWith(NAB_SETTING + ".");
  private static final Set<String> SUPPORTED_PROPERTIES = Set.of(
      SERVICE_NAME,
      KafkaStatsDReporter.STATSD_INSTANCE_PROPERTY,
      KafkaStatsDReporter.METRICS_ALLOWED,
      KafkaStatsDReporter.METRICS_SEND_ALL
  );
  private final String serviceName;
  private final String kafkaClusterName;
  private final Properties properties;
  private final StatsDSender statsDSender;

  public ConfigProvider(String serviceName, String kafkaClusterName, Properties properties, StatsDSender statsDSender) {
    this.serviceName = serviceName;
    this.kafkaClusterName = kafkaClusterName;
    this.properties = properties;
    this.statsDSender = statsDSender;
  }

  private static void checkNames(Map<String, ?> nonNabSettings, Set<String> supportedNames, String type) {
    SortedSet<String> invalidNames = nonNabSettings
        .keySet()
        .stream()
        .filter(key -> !supportedNames.contains(key))
        .collect(Collectors.toCollection(TreeSet::new));

    if (!invalidNames.isEmpty()) {
      throw new IllegalArgumentException(
          String.format("Unsupported kafka %s properties found: '%s'", type, String.join("', '", invalidNames))
      );
    }
  }

  private static void checkConsumerNames(Map<String, ?> nonNabConsumerSettings) {
    Set<String> supportedNames = new HashSet<>(ConsumerConfig.configNames());
    supportedNames.addAll(SUPPORTED_PROPERTIES);
    checkNames(nonNabConsumerSettings, supportedNames, "consumer");
  }

  private static void checkProducerNames(Map<String, ?> nonNabProducerSettings) {
    Set<String> supportedNames = new HashSet<>(ProducerConfig.configNames());
    supportedNames.addAll(SUPPORTED_PROPERTIES);
    checkNames(nonNabProducerSettings, supportedNames, "producer");
  }

  public String getServiceName() {
    return serviceName;
  }

  public String getKafkaClusterName() {
    return kafkaClusterName;
  }

  public Map<String, Object> getConsumerConfig(String topicName) {
    Map<String, Object> consumerConfig = new HashMap<>();
    consumerConfig.put(ConsumerConfig.CLIENT_ID_CONFIG, serviceName);
    consumerConfig.putAll(getAllConsumerConfigs(topicName));
    removeNabProperties(consumerConfig);

    checkConsumerNames(consumerConfig);
    return consumerConfig;
  }

  public Properties getNabConsumerSettings(String topicName) {
    Map<String, Object> allConsumerConfigs = getAllConsumerConfigs(topicName);

    Properties nabProperties = new Properties();
    nabProperties.putAll(allConsumerConfigs);
    removeNonNabProperties(nabProperties);
    Properties nabConsumerSettings = PropertiesUtils.getSubProperties(nabProperties, NAB_SETTING);

    Properties nonNabProperties = new Properties();
    nonNabProperties.putAll(allConsumerConfigs);
    removeNabProperties(nonNabProperties);

    checkConsumerConfig(nabConsumerSettings, nonNabProperties);
    return nabConsumerSettings;
  }

  private Map<String, Object> getAllConsumerConfigs(String topicName) {
    Map<String, Object> consumerConfig = new HashMap<>();
    consumerConfig.put(ENABLE_AUTO_COMMIT_CONFIG, false);
    consumerConfig.putAll(getCommonProperties());
    consumerConfig.putAll(getDefaultConsumerProperties());
    consumerConfig.putAll(getTopicOverriddenConsumerProperties(topicName));
    return consumerConfig;
  }

  private void checkConsumerConfig(Properties nabConsumerSettings, Properties nonNabConsumerSettings) {
    long maxPollMs = PropertiesUtils.getLong(nonNabConsumerSettings, MAX_POLL_INTERVAL_MS_CONFIG, 300000L);
    long backoffMaxInterval = PropertiesUtils.getLong(nabConsumerSettings, BACKOFF_MAX_INTERVAL_NAME, DEFAULT_BACKOFF_MAX_INTERVAL);
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
    findAnyMatchedKey(String.format(
        TOPIC_CONSUMER_INVALID_CONFIG_REGEXP_TEMPLATE, kafkaClusterName, topicName))
        .ifPresent(key -> {
          throw new IllegalArgumentException(
              String.format("Unused property found: '%s'", key)
          );
        });
    return getConfigAsMap(String.format(TOPIC_CONSUMER_CONFIG_TEMPLATE, kafkaClusterName, topicName));
  }

  private Optional<String> findAnyMatchedKey(String pattern) {
    Pattern compiledPattern = Pattern.compile(pattern);
    return properties
        .stringPropertyNames()
        .stream()
        .filter(key -> compiledPattern.matcher(key).matches())
        .findAny();
  }

  public Map<String, Object> getDefaultProducerConfig() {
    return getProducerConfig(DEFAULT_PRODUCER_NAME);
  }

  public Map<String, Object> getProducerConfig(String producerName) {
    Map<String, Object> producerConfig = new HashMap<>();
    producerConfig.put(ProducerConfig.CLIENT_ID_CONFIG, serviceName);
    producerConfig.putAll(getCommonProperties());
    producerConfig.putAll(getDefaultProducerProperties(producerName));

    checkProducerNames(producerConfig);
    return producerConfig;
  }

  private Map<String, Object> getDefaultProducerProperties(String producerName) {
    return getConfigAsMap(String.format(PRODUCER_CONFIG_TEMPLATE, kafkaClusterName, producerName));
  }

  private Map<String, Object> getCommonProperties() {
    Map<String, Object> properties = getConfigAsMap(String.format(COMMON_CONFIG_TEMPLATE, kafkaClusterName));
    properties.put(SERVICE_NAME, serviceName);

    String metricReporters = ofNullable(properties.get(CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG))
        .map(Object::toString)
        .orElseGet(KafkaStatsDReporter.class::getName);
    properties.put(CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG, metricReporters);

    String metricsSendAll = ofNullable(properties.get(KafkaStatsDReporter.METRICS_SEND_ALL))
        .map(Object::toString)
        .orElseGet(Boolean.FALSE::toString);
    properties.put(KafkaStatsDReporter.METRICS_SEND_ALL, metricsSendAll);

    String enabledMetrics = ofNullable(properties.get(KafkaStatsDReporter.METRICS_ALLOWED))
        .map(Object::toString)
        .orElse("");
    properties.put(KafkaStatsDReporter.METRICS_ALLOWED, enabledMetrics);

    // TODO Remove when we leave Okmeter monitoring
    // Okmeter doesn't provide precision better than once a minute
    properties.put(ConsumerConfig.METRICS_SAMPLE_WINDOW_MS_CONFIG, 60000);
    // Approximation, kafka defaults are 30000ms for sample window and 2 for num samples
    properties.put(ConsumerConfig.METRICS_NUM_SAMPLES_CONFIG, 4);
    // A workaround to support single instance of StatsD client
    properties.put(KafkaStatsDReporter.STATSD_INSTANCE_PROPERTY, this.statsDSender);
    return properties;
  }

  private Map<String, Object> getConfigAsMap(String prefix) {
    Properties subProperties = PropertiesUtils.getSubProperties(properties, prefix);
    return new HashMap<>(PropertiesUtils.getAsMap(subProperties));
  }

  private void removeNabProperties(Map<?, ?> config) {
    config.keySet().removeIf(NAB_SETTING_PREDICATE);
  }

  private void removeNonNabProperties(Properties allProperties) {
    allProperties.keySet().removeIf(NAB_SETTING_PREDICATE.negate());
  }
}
