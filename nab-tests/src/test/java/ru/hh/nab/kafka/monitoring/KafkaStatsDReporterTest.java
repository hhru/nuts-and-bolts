package ru.hh.nab.kafka.monitoring;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.kafka.KafkaTestConfig;
import ru.hh.nab.kafka.consumer.KafkaConsumer;
import ru.hh.nab.kafka.consumer.KafkaConsumerTestbase;
import ru.hh.nab.kafka.consumer.TopicConsumerMock;
import static ru.hh.nab.kafka.monitoring.KafkaStatsDReporter.ReporterTag.CLIENT_ID;
import static ru.hh.nab.kafka.monitoring.KafkaStatsDReporter.ReporterTag.NODE_ID;
import static ru.hh.nab.kafka.monitoring.KafkaStatsDReporter.ReporterTag.PARTITION;
import static ru.hh.nab.kafka.monitoring.KafkaStatsDReporter.ReporterTag.TOPIC;
import ru.hh.nab.kafka.producer.KafkaProducer;
import ru.hh.nab.kafka.producer.KafkaProducerFactory;
import ru.hh.nab.kafka.producer.KafkaSendResult;
import ru.hh.nab.kafka.util.ConfigProvider;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.starter.qualifier.Service;

@ContextConfiguration(classes = {KafkaStatsDReporterTest.CompanionConfiguration.class})
class KafkaStatsDReporterTest extends KafkaConsumerTestbase {
  private TopicConsumerMock<String> consumerMock;
  private KafkaConsumer<String> consumer;

  @Inject
  private KafkaProducerFactory producerFactory;

  private static final String DEFAULT_METRIC_GROUP = "consumer-metrics";
  private static final String DEFAULT_METRIC_NAME = "outgoing-byte-total";
  private static final AtomicReference<ConcurrentMap<MetricName, Metric>> observedMetrics = new AtomicReference<>();

  @BeforeEach
  public void setUp() {
    consumerMock = new TopicConsumerMock<>();
  }

  @AfterEach
  public void tearDown() {
    if (consumer != null) {
      consumer.stop();
    }
  }

  /**
   * Verifies that Kafka metric tags are similar to StatsD tags
   * This restriction allows developers to use official documentation
   * https://kafka.apache.org/31/documentation.html#common_node_monitoring
   */
  @Test
  public void verifyEquality() {
    for (KafkaStatsDReporter.ReporterTag value : KafkaStatsDReporter.ReporterTag.values()) {
      assertEquals(formatKafkaTag(value.getKafkaTag()), formatStatsDTag(value.getStatsDTag()));
    }
  }

  private String formatKafkaTag(String tagName) {
    return tagName.replace("-", ".");
  }

  private String formatStatsDTag(String tagName) {
    return tagName.replace("_", ".");
  }

  /**
   * Verifies that native kafka MetricReporter interface sends metrics
   */
  @Test
  public void sendMetrics() {
    consumer = startMessagesConsumer(String.class, consumerMock);

    String payload = UUID.randomUUID().toString();
    kafkaTestUtils.sendMessage(topicName, payload);

    var observedMetric = getObservedMetric(DEFAULT_METRIC_NAME, DEFAULT_METRIC_GROUP);
    Assertions.assertNotEquals((double) observedMetric.metricValue(), 0, 0.1);
  }

  /**
   * Verifies that native kafka MetricReporter interface sends updates for metrics
   */
  @Test
  public void sendUpdatesForMetrics() {
    consumer = startMessagesConsumer(String.class, consumerMock);

    String payload = UUID.randomUUID().toString();
    kafkaTestUtils.sendMessage(topicName, payload);

    var observedMetric = getObservedMetric(DEFAULT_METRIC_NAME, DEFAULT_METRIC_GROUP);
    double earliestObservedValue = (double) observedMetric.metricValue();
    Assertions.assertNotEquals(earliestObservedValue, 0, 0.1);
    kafkaTestUtils.sendMessage(topicName, payload);

    Awaitility.await().atMost(Duration.ofSeconds(5L)).untilAsserted(() -> {
      double latestObservedValue = (double) observedMetric.metricValue();
      Assertions.assertNotEquals(latestObservedValue, earliestObservedValue, 0.1);
    });
  }

  /**
   * Verifies that tags used for producer monitoring are still reported by kafka client
   */
  @Test
  public void sendTagsForProducerMetrics() {
    consumer = startMessagesConsumer(String.class, consumerMock);

    KafkaProducer defaultProducer = producerFactory.createDefaultProducer();
    String payload = UUID.randomUUID().toString();
    CompletableFuture<KafkaSendResult<String>> future = defaultProducer.sendMessage(topicName, payload);
    try {
      future.get(1, TimeUnit.SECONDS);
    } catch (Exception e) {
      Assertions.fail("Client must send message to receive metrics.", e);
    }

    var observedMetric = getObservedMetric("buffer-total-bytes", "producer-metrics");
    Assertions.assertTrue(observedMetric.metricName().tags().containsKey(CLIENT_ID.getKafkaTag()));

    var nodeMetricGroup = "producer-node-metrics";
    var nodeMetricName = "incoming-byte-rate";
    observedMetric = getObservedMetric(nodeMetricName, nodeMetricGroup);
    Assertions.assertTrue(observedMetric.metricName().tags().containsKey(CLIENT_ID.getKafkaTag()));
    Assertions.assertTrue(observedMetric.metricName().tags().containsKey(NODE_ID.getKafkaTag()));

    var topicMetricGroup = "producer-topic-metrics";
    var topicMetricName = "byte-rate";
    observedMetric = getObservedMetric(topicMetricName, topicMetricGroup);
    Assertions.assertTrue(observedMetric.metricName().tags().containsKey(CLIENT_ID.getKafkaTag()));
    Assertions.assertTrue(observedMetric.metricName().tags().containsKey(TOPIC.getKafkaTag()));
  }

  /**
   * Verifies that tags used for consumer monitoring are still reported by kafka client
   */
  @Test
  public void sendTagsForConsumerMetrics() {
    consumer = startMessagesConsumer(String.class, consumerMock);

    String payload = UUID.randomUUID().toString();
    kafkaTestUtils.sendMessage(topicName, payload);

    var observedMetric = getObservedMetric(DEFAULT_METRIC_NAME, DEFAULT_METRIC_GROUP);
    Assertions.assertTrue(observedMetric.metricName().tags().containsKey(CLIENT_ID.getKafkaTag()));

    var nodeMetricGroup = "consumer-node-metrics";
    var nodeMetricName = "incoming-byte-rate";
    observedMetric = getObservedMetric(nodeMetricName, nodeMetricGroup);
    Assertions.assertTrue(observedMetric.metricName().tags().containsKey(CLIENT_ID.getKafkaTag()));
    Assertions.assertTrue(observedMetric.metricName().tags().containsKey(NODE_ID.getKafkaTag()));

    var coordinatorMetricGroup = "consumer-coordinator-metrics";
    var coordinatorMetricName = "assigned-partitions";
    observedMetric = getObservedMetric(coordinatorMetricName, coordinatorMetricGroup);
    Assertions.assertTrue(observedMetric.metricName().tags().containsKey(CLIENT_ID.getKafkaTag()));

    var fetchManagerMetricGroup = "consumer-fetch-manager-metrics";
    var fetchManagerMetricName = "preferred-read-replica";
    observedMetric = getObservedMetric(fetchManagerMetricName, fetchManagerMetricGroup);
    Assertions.assertTrue(observedMetric.metricName().tags().containsKey(CLIENT_ID.getKafkaTag()));
    Assertions.assertTrue(observedMetric.metricName().tags().containsKey(TOPIC.getKafkaTag()));
    Assertions.assertTrue(observedMetric.metricName().tags().containsKey(PARTITION.getKafkaTag()));
  }

  private static Metric getObservedMetric(String metricName, String metricGroup) {
    ConcurrentMap<MetricName, Metric> metrics = observedMetrics.get();
    Metric metric = metrics
        .entrySet()
        .stream()
        .filter(entry -> {
          MetricName key = entry.getKey();
          return key.name().equals(metricName) && key.group().equals(metricGroup);
        })
        .map(Map.Entry::getValue)
        .findFirst()
        .orElseThrow();
    return Objects.requireNonNull(metric, "Initialize kafka client before using metrics");
  }

  // Public visibility and default constructor is necessary for kafka client
  public static class TestMetricsReporter extends KafkaStatsDReporter {
    @Override
    public void configure(Map<String, ?> configs) {
      super.configure(configs);
      KafkaStatsDReporterTest.observedMetrics.set(this.recordedMetrics);
    }
  }

  @Configuration
  protected static class CompanionConfiguration extends KafkaTestConfig {
    @Primary
    @Bean
    ConfigProvider configProvider(@Service Properties serviceProperties, StatsDSender statsDSender) {
      String clusterName = "kafka";
      // See ru.hh.nab.kafka.util.ConfigProvider.COMMON_CONFIG_TEMPLATE
      String prefix = "%s.common".formatted(clusterName);
      serviceProperties.put(prefix + "." + CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG, TestMetricsReporter.class.getName());
      return new ConfigProvider("service", clusterName, new FileSettings(serviceProperties), statsDSender);
    }
  }
}
