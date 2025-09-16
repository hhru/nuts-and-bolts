package ru.hh.nab.kafka.monitoring;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.nab.kafka.KafkaTestConfig;
import ru.hh.nab.kafka.consumer.KafkaConsumer;
import ru.hh.nab.kafka.consumer.KafkaConsumerTestBase;
import ru.hh.nab.kafka.consumer.TopicConsumerMock;
import static ru.hh.nab.kafka.monitoring.KafkaStatsDReporter.ReporterTag.CLIENT_ID;
import static ru.hh.nab.kafka.monitoring.KafkaStatsDReporter.ReporterTag.NODE_ID;
import static ru.hh.nab.kafka.monitoring.KafkaStatsDReporter.ReporterTag.PARTITION;
import static ru.hh.nab.kafka.monitoring.KafkaStatsDReporter.ReporterTag.TOPIC;
import ru.hh.nab.kafka.producer.KafkaProducer;
import ru.hh.nab.kafka.producer.KafkaProducerFactory;
import ru.hh.nab.kafka.producer.KafkaSendResult;

@SpringBootTest(classes = KafkaStatsDReporterTest.CompanionConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class KafkaStatsDReporterTest extends KafkaConsumerTestBase {
  private static final MetricName CONSUMER_METRICS_OUTGOING_BYTE_TOTAL =
      new MetricName("outgoing-byte-total", "consumer-metrics", "", Collections.emptyMap());
  private static final MetricName CONSUMER_NODE_METRICS_INCOMING_BYTE_RATE =
      new MetricName("incoming-byte-rate", "consumer-node-metrics", "", Collections.emptyMap());
  private static final MetricName CONSUMER_COORDINATOR_METRICS_ASSIGNED_PARTITIONS =
      new MetricName("assigned-partitions", "consumer-coordinator-metrics", "", Collections.emptyMap());
  private static final MetricName CONSUMER_FETCH_MANAGER_METRICS_PREFERRED_READ_REPLICA =
      new MetricName("preferred-read-replica", "consumer-fetch-manager-metrics", "", Collections.emptyMap());
  private static final MetricName PRODUCER_METRICS_BUFFER_TOTAL_BYTES =
      new MetricName("buffer-total-bytes", "producer-metrics", "", Collections.emptyMap());
  private static final MetricName PRODUCER_NODE_METRICS_INCOMING_BYTE_RATE =
      new MetricName("incoming-byte-rate", "producer-node-metrics", "", Collections.emptyMap());
  private static final MetricName PRODUCER_TOPIC_METRICS_BYTE_RATE =
      new MetricName("byte-rate", "producer-topic-metrics", "", Collections.emptyMap());
  private static final Set<MetricName> ENABLED_METRICS = Set.of(
      CONSUMER_METRICS_OUTGOING_BYTE_TOTAL,
      CONSUMER_NODE_METRICS_INCOMING_BYTE_RATE,
      CONSUMER_COORDINATOR_METRICS_ASSIGNED_PARTITIONS,
      CONSUMER_FETCH_MANAGER_METRICS_PREFERRED_READ_REPLICA,
      PRODUCER_METRICS_BUFFER_TOTAL_BYTES,
      PRODUCER_NODE_METRICS_INCOMING_BYTE_RATE,
      PRODUCER_TOPIC_METRICS_BYTE_RATE
  );
  private static final AtomicReference<ConcurrentMap<MetricName, Metric>> observedMetrics = new AtomicReference<>();
  private TopicConsumerMock<String> consumerMock;
  private KafkaConsumer<String> consumer;
  @Inject
  private KafkaProducerFactory producerFactory;

  private static Metric getObservedMetric(MetricName searchedMetricName) {
    ConcurrentMap<MetricName, Metric> metrics = observedMetrics.get();

    String nameToSearch = KafkaStatsDReporter.createMetricName(searchedMetricName);
    for (MetricName originalMetricName : metrics.keySet()) {
      String originalName = KafkaStatsDReporter.createMetricName(originalMetricName);
      if (originalName.equals(nameToSearch)) {
        return Objects.requireNonNull(metrics.get(originalMetricName), "Initialize kafka client before using metrics");
      }
    }

    throw new RuntimeException("Metric [%s] was not found.".formatted(nameToSearch));
  }

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
    testKafka.sendMessage(topicName, payload);

    var observedMetric = getObservedMetric(CONSUMER_METRICS_OUTGOING_BYTE_TOTAL);
    Assertions.assertNotEquals((double) observedMetric.metricValue(), 0, 0.1);
  }

  /**
   * Verifies that native kafka MetricReporter interface sends updates for metrics
   */
  @Test
  public void sendUpdatesForMetrics() {
    consumer = startMessagesConsumer(String.class, consumerMock);

    String payload = UUID.randomUUID().toString();
    testKafka.sendMessage(topicName, payload);

    var observedMetric = getObservedMetric(CONSUMER_METRICS_OUTGOING_BYTE_TOTAL);
    double earliestObservedValue = (double) observedMetric.metricValue();
    Assertions.assertNotEquals(earliestObservedValue, 0, 0.1);
    testKafka.sendMessage(topicName, payload);

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

    var observedMetric = getObservedMetric(PRODUCER_METRICS_BUFFER_TOTAL_BYTES);
    Assertions.assertTrue(observedMetric.metricName().tags().containsKey(CLIENT_ID.getKafkaTag()));

    observedMetric = getObservedMetric(PRODUCER_NODE_METRICS_INCOMING_BYTE_RATE);
    Assertions.assertTrue(observedMetric.metricName().tags().containsKey(CLIENT_ID.getKafkaTag()));
    Assertions.assertTrue(observedMetric.metricName().tags().containsKey(NODE_ID.getKafkaTag()));

    observedMetric = getObservedMetric(PRODUCER_TOPIC_METRICS_BYTE_RATE);
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
    testKafka.sendMessage(topicName, payload);

    var observedMetric = getObservedMetric(CONSUMER_METRICS_OUTGOING_BYTE_TOTAL);
    Assertions.assertTrue(observedMetric.metricName().tags().containsKey(CLIENT_ID.getKafkaTag()));

    observedMetric = getObservedMetric(CONSUMER_NODE_METRICS_INCOMING_BYTE_RATE);
    Assertions.assertTrue(observedMetric.metricName().tags().containsKey(CLIENT_ID.getKafkaTag()));
    Assertions.assertTrue(observedMetric.metricName().tags().containsKey(NODE_ID.getKafkaTag()));

    observedMetric = getObservedMetric(CONSUMER_COORDINATOR_METRICS_ASSIGNED_PARTITIONS);
    Assertions.assertTrue(observedMetric.metricName().tags().containsKey(CLIENT_ID.getKafkaTag()));

    observedMetric = getObservedMetric(CONSUMER_FETCH_MANAGER_METRICS_PREFERRED_READ_REPLICA);
    Assertions.assertTrue(observedMetric.metricName().tags().containsKey(CLIENT_ID.getKafkaTag()));
    Assertions.assertTrue(observedMetric.metricName().tags().containsKey(TOPIC.getKafkaTag()));
    Assertions.assertTrue(observedMetric.metricName().tags().containsKey(PARTITION.getKafkaTag()));
  }

  /**
   * Verifies that only enabled metrics are sent
   */
  @Test
  public void sendOnlyEnabledMetrics() {
    consumer = startMessagesConsumer(String.class, consumerMock);

    String payload = UUID.randomUUID().toString();
    testKafka.sendMessage(topicName, payload);


    Set<String> enabledMetricsNames = ENABLED_METRICS
        .stream()
        .map(KafkaStatsDReporter::createMetricName)
        .collect(Collectors.toSet());

    ConcurrentMap<MetricName, Metric> observedMetrics = KafkaStatsDReporterTest.observedMetrics.get();
    for (MetricName metricName : observedMetrics.keySet()) {
      String originalName = KafkaStatsDReporter.createMetricName(metricName);
      Assertions.assertTrue(enabledMetricsNames.contains(originalName) || KafkaStatsDReporter.CRITICAL_METRICS.contains(originalName));
    }
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
  public static class CompanionConfiguration extends KafkaTestConfig {
    @Bean
    @Named(KAFKA)
    public Properties properties() {
      Properties properties = super.properties();
      // See ru.hh.nab.kafka.util.ConfigProvider.COMMON_CONFIG_TEMPLATE
      String prefix = "kafka.common";
      properties.put(prefix + "." + CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG, TestMetricsReporter.class.getName());
      properties.put(
          prefix + "." + KafkaStatsDReporter.METRICS_ALLOWED,
          ENABLED_METRICS
              .stream()
              .map(KafkaStatsDReporter::createMetricName)
              .collect(Collectors.joining(","))
      );
      return properties;
    }
  }
}
