package ru.hh.nab.kafka.util;

import com.timgroup.statsd.NonBlockingStatsDClient;
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
import static org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG;
import org.apache.kafka.clients.producer.ProducerConfig;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import ru.hh.nab.kafka.monitoring.KafkaStatsDReporter;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_BUFFER_POOL_SIZE_ENV;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_BUFFER_POOL_SIZE_PROPERTY;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_DEFAULT_PERIODIC_SEND_INTERVAL;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_HOST_ENV;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_HOST_PROPERTY;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_MAX_PACKET_SIZE_BYTES_ENV;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_MAX_PACKET_SIZE_BYTES_PROPERTY;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_PORT_ENV;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_PORT_PROPERTY;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_QUEUE_SIZE_ENV;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_QUEUE_SIZE_PROPERTY;
import ru.hh.nab.metrics.StatsDSender;

public class ConfigProvider {
  static final String COMMON_CONFIG_TEMPLATE = "%s.common";
  static final String DEFAULT_CONSUMER_CONFIG_TEMPLATE = "%s.consumer.default";
  static final String TOPIC_CONSUMER_CONFIG_TEMPLATE = "%s.consumer.topic.%s.default";
  static final String TOPIC_CONSUMER_INVALID_CONFIG_REGEXP_TEMPLATE =
      "%s\\.consumer\\.topic\\.%s\\.(?!\\w*(?:default)).*";
  static final String PRODUCER_CONFIG_TEMPLATE = "%s.producer.%s";
  public static final String NAB_SETTING = "nab_setting";
  static final Predicate<Object> NAB_SETTING_PREDICATE = key -> ((String) key).startsWith(NAB_SETTING + ".");
  public static final String DEFAULT_PRODUCER_NAME = "default";

  public static final String BACKOFF_INITIAL_INTERVAL_NAME = "backoff.initial.interval";
  public static final long DEFAULT_BACKOFF_INITIAL_INTERVAL = 1000L;
  public static final String BACKOFF_MAX_INTERVAL_NAME = "backoff.max.interval";
  public static final long DEFAULT_BACKOFF_MAX_INTERVAL = 60000L;
  public static final String BACKOFF_MULTIPLIER_NAME = "backoff.multiplier";
  public static final double DEFAULT_BACKOFF_MULTIPLIER = 1.5;

  public static final String POLL_TIMEOUT = "poll.timeout.ms";
  public static final long DEFAULT_POLL_TIMEOUT_MS = 5000L;

  public static final String AUTH_EXCEPTION_RETRY_INTERVAL = "auth.exception.retry.interval.ms";
  public static final long DEFAULT_AUTH_EXCEPTION_RETRY_INTERVAL_MS = 10000L;
  public static final String CONCURRENCY = "concurrency";

  private final String serviceName;
  private final String kafkaClusterName;
  private final FileSettings fileSettings;

  private static final Set<String> SUPPORTED_PROPERTIES = Set.of(
      SERVICE_NAME,
      STATSD_HOST_PROPERTY,
      STATSD_PORT_PROPERTY,
      STATSD_QUEUE_SIZE_PROPERTY,
      STATSD_MAX_PACKET_SIZE_BYTES_PROPERTY,
      STATSD_BUFFER_POOL_SIZE_PROPERTY,
      STATSD_DEFAULT_PERIODIC_SEND_INTERVAL
  );

  public ConfigProvider(String serviceName, String kafkaClusterName, FileSettings fileSettings) {
    this.serviceName = serviceName;
    this.kafkaClusterName = kafkaClusterName;
    this.fileSettings = fileSettings;
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

  public FileSettings getNabConsumerSettings(String topicName) {
    Map<String, Object> allConsumerConfigs = getAllConsumerConfigs(topicName);

    Properties nabProperties = new Properties();
    nabProperties.putAll(allConsumerConfigs);
    removeNonNabProperties(nabProperties);
    FileSettings nabConsumerSettings = new FileSettings(nabProperties).getSubSettings(NAB_SETTING);

    Properties nonNabProperties = new Properties();
    nonNabProperties.putAll(allConsumerConfigs);
    removeNabProperties(nonNabProperties);
    FileSettings nonNabConsumerSettings = new FileSettings(nonNabProperties);

    checkConsumerConfig(nabConsumerSettings, nonNabConsumerSettings);
    return nabConsumerSettings;
  }

  private Map<String, Object> getAllConsumerConfigs(String topicName) {
    Map<String, Object> consumerConfig = new HashMap<>();
    consumerConfig.putAll(getCommonProperties());
    consumerConfig.putAll(getDefaultConsumerProperties());
    consumerConfig.putAll(getTopicOverriddenConsumerProperties(topicName));
    return consumerConfig;
  }

  private void checkConsumerConfig(FileSettings nabConsumerSettings, FileSettings nonNabConsumerSettings) {
    long maxPollMs = nonNabConsumerSettings.getLong(MAX_POLL_INTERVAL_MS_CONFIG, 300000L);
    long backoffMaxInterval = nabConsumerSettings.getLong(BACKOFF_MAX_INTERVAL_NAME, DEFAULT_BACKOFF_MAX_INTERVAL);
    if (backoffMaxInterval > maxPollMs) {
      throw new IllegalArgumentException(
          String.format("'%s' should not be larger then '%s'", BACKOFF_MAX_INTERVAL_NAME, MAX_POLL_INTERVAL_MS_CONFIG)
      );
    }
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
    return fileSettings
        .getProperties()
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

    String host = ofNullable(fileSettings.getString(STATSD_HOST_PROPERTY))
        .or(() -> ofNullable(System.getProperty(STATSD_HOST_ENV)))
        .orElse("localhost");
    properties.put(STATSD_HOST_PROPERTY, host);

    int port = ofNullable(fileSettings.getString(STATSD_PORT_PROPERTY))
        .or(() -> ofNullable(System.getProperty(STATSD_PORT_ENV)))
        .map(Integer::parseInt)
        .orElse(8125);
    properties.put(STATSD_PORT_PROPERTY, port);

    int queueSize = ofNullable(fileSettings.getString(STATSD_QUEUE_SIZE_PROPERTY))
        .or(() -> ofNullable(System.getProperty(STATSD_QUEUE_SIZE_ENV)))
        .map(Integer::parseInt)
        .orElse(10_000);
    properties.put(STATSD_QUEUE_SIZE_PROPERTY, queueSize);

    int maxPacketSizeBytes = ofNullable(fileSettings.getString(STATSD_MAX_PACKET_SIZE_BYTES_PROPERTY))
        .or(() -> ofNullable(System.getProperty(STATSD_MAX_PACKET_SIZE_BYTES_ENV)))
        .map(Integer::parseInt)
        .orElse(NonBlockingStatsDClient.DEFAULT_UDP_MAX_PACKET_SIZE_BYTES);
    properties.put(STATSD_MAX_PACKET_SIZE_BYTES_PROPERTY, maxPacketSizeBytes);

    int bufferPoolSize = ofNullable(fileSettings.getString(STATSD_BUFFER_POOL_SIZE_PROPERTY))
        .or(() -> ofNullable(System.getProperty(STATSD_BUFFER_POOL_SIZE_ENV)))
        .map(Integer::parseInt)
        .orElse(8);
    properties.put(STATSD_BUFFER_POOL_SIZE_PROPERTY, bufferPoolSize);

    int sendIntervalSeconds = ofNullable(fileSettings.getInteger(STATSD_DEFAULT_PERIODIC_SEND_INTERVAL))
        .orElse(StatsDSender.DEFAULT_SEND_INTERVAL_SECONDS);
    properties.put(STATSD_DEFAULT_PERIODIC_SEND_INTERVAL, sendIntervalSeconds);

    // TODO Remove when we leave Okmeter monitoring
    // Okmeter doesn't provide precision better than once a minute
    properties.put(ConsumerConfig.METRICS_SAMPLE_WINDOW_MS_CONFIG, 60000);
    // Approximation, kafka defaults are 30000ms for sample window and 2 for num samples
    properties.put(ConsumerConfig.METRICS_NUM_SAMPLES_CONFIG, 4);
    return properties;
  }

  private Map<String, Object> getConfigAsMap(String prefix) {
    return new HashMap<>(fileSettings.getSubSettings(prefix).getAsMap());
  }

  private void removeNabProperties(Map<?, ?> config) {
    config.keySet().removeIf(NAB_SETTING_PREDICATE);
  }

  private void removeNonNabProperties(Properties allProperties) {
    allProperties.keySet().removeIf(NAB_SETTING_PREDICATE.negate());
  }
}
