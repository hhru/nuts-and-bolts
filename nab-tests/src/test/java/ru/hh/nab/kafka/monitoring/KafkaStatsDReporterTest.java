package ru.hh.nab.kafka.monitoring;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
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
import ru.hh.nab.kafka.util.ConfigProvider;
import ru.hh.nab.starter.qualifier.Service;

@ContextConfiguration(classes = {KafkaStatsDReporterTest.CompanionConfiguration.class})
class KafkaStatsDReporterTest extends KafkaConsumerTestbase {
  private TopicConsumerMock<String> consumerMock;
  private KafkaConsumer<String> consumer;

  private static final String observedMetricGroup = "consumer-metrics";
  private static final String observedMetricName = "outgoing-byte-total";
  private static final AtomicReference<ConcurrentMap<MetricName, Metric>> observedMetrics = new AtomicReference<>();

  @BeforeEach
  public void setUp() {
    consumerMock = new TopicConsumerMock<>();
  }

  @AfterEach
  public void tearDown() {
    consumer.stop();
  }

  /**
   * Verifies that native kafka MetricReporter interface sends metrics
   */
  @Test
  public void sendMetrics() {
    consumer = startMessagesConsumer(String.class, consumerMock);

    String payload = UUID.randomUUID().toString();
    kafkaTestUtils.sendMessage(topicName, payload);

    var observedMetric = getObservedMetric();
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

    var observedMetric = getObservedMetric();
    double earliestObservedValue = (double) observedMetric.metricValue();
    Assertions.assertNotEquals(earliestObservedValue, 0, 0.1);
    kafkaTestUtils.sendMessage(topicName, payload);

    Awaitility.await().atMost(Duration.ofSeconds(5L)).untilAsserted(() -> {
      double latestObservedValue = (double) observedMetric.metricValue();
      Assertions.assertNotEquals(latestObservedValue, earliestObservedValue, 0.1);
    });
  }

  private static Metric getObservedMetric() {
    ConcurrentMap<MetricName, Metric> metrics = observedMetrics.get();
    Metric metric = metrics
        .entrySet()
        .stream()
        .filter(entry -> {
          MetricName key = entry.getKey();
          return key.name().equals(observedMetricName) && key.group().equals(observedMetricGroup);
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
    ConfigProvider configProvider(@Service Properties serviceProperties) {
      String clusterName = "kafka";
      // See ru.hh.nab.kafka.util.ConfigProvider.COMMON_CONFIG_TEMPLATE
      String prefix = "%s.common".formatted(clusterName);
      serviceProperties.put(prefix + "." + CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG, TestMetricsReporter.class.getName());
      return new ConfigProvider("service", clusterName, new FileSettings(serviceProperties));
    }
  }
}
