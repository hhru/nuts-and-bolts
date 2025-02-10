package ru.hh.nab.kafka.monitoring;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import static java.util.Optional.ofNullable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.MetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.Tag;

public class KafkaStatsDReporter implements MetricsReporter {
  public static final String METRICS_ALLOWED = "metrics.allowed";
  public static final String METRICS_SEND_ALL = "metrics.send-all";
  public static final String STATSD_INSTANCE_PROPERTY = "NAB_STATSD_INSTANCE";
  static final Set<String> CRITICAL_METRICS = Set.of(
      "consumer-metrics.last-poll-seconds-ago",

      "consumer-fetch-manager-metrics.records-lag",
      "consumer-fetch-manager-metrics.records-consumed-rate",
      "consumer-fetch-manager-metrics.records-per-request-avg",
      "consumer-fetch-manager-metrics.fetch-rate",
      "consumer-fetch-manager-metrics.fetch-latency-max",
      "consumer-fetch-manager-metrics.fetch-throttle-time-max",
      "consumer-fetch-manager-metrics.bytes-consumed-rate",

      "consumer-coordinator-metrics.commit-latency-max",
      "consumer-coordinator-metrics.assigned-partitions",
      "consumer-coordinator-metrics.commit-rate",
      "consumer-coordinator-metrics.last-rebalance-seconds-ago",
      "consumer-coordinator-metrics.last-heartbeat-seconds-ago",

      "producer-metrics.request-latency-avg",
      "producer-metrics.requests-in-flight",
      "producer-metrics.batch-size-max",
      "producer-metrics.records-per-request-avg",
      "producer-metrics.record-queue-time-avg",
      "producer-metrics.compression-rate-avg",
      "producer-metrics.produce-throttle-time-max",

      "producer-topic-metrics.record-send-rate",
      "producer-topic-metrics.record-retry-rate",
      "producer-topic-metrics.record-error-rate"
  );
  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaStatsDReporter.class);
  protected final ConcurrentMap<MetricName, Metric> recordedMetrics = new ConcurrentHashMap<>();
  private String serviceName;
  private StatsDSender statsDSender;
  private boolean isSendAll;
  private Set<String> allowedMetrics;

  // Visible only for tests
  static String createMetricName(MetricName metricName) {
    return String.format("%s.%s", metricName.group(), metricName.name());
  }

  @Override
  public void init(List<KafkaMetric> metrics) {
    for (KafkaMetric metric : metrics) {
      recordMetric(metric);
    }

    statsDSender.sendPeriodically(() -> {
      recordedMetrics.forEach((key, value) -> {
        try {
          Object metricValue = value.metricValue();
          Map<String, String> tags = key.tags();

          Tag serviceNameTag = new Tag(Tag.APP_TAG_NAME, this.serviceName);
          Tag nodeIdTag = createTag(tags, ReporterTag.NODE_ID);
          Tag clientIdTag = createTag(tags, ReporterTag.CLIENT_ID);

          String name = createMetricName(key);
          if (metricValue instanceof Number number) {
            Tag topicTag = createTag(tags, ReporterTag.TOPIC);
            Tag partitionTag = createTag(tags, ReporterTag.PARTITION);

            statsDSender.sendGauge(name, number.doubleValue(), serviceNameTag, nodeIdTag, clientIdTag, topicTag, partitionTag);
            LOGGER.debug("Sent gauge value {} for metric {}", value, name);
          } else {
            statsDSender.sendSetValue(name, metricValue.toString(), serviceNameTag, nodeIdTag, clientIdTag);
            LOGGER.debug("Sent set value {} for metric {}", value, name);
          }
        } catch (Exception e) {
          LOGGER.error("Skipping metric %s".formatted(key), e);
        }
      });
    });
  }

  private Tag createTag(Map<String, String> tags, ReporterTag tag) {
    String kafkaTag = tags.getOrDefault(tag.getKafkaTag(), "unknown");
    return new Tag(tag.getStatsDTag(), kafkaTag);
  }

  @Override
  public void metricChange(KafkaMetric metric) {
    recordMetric(metric);
  }

  @Override
  public void metricRemoval(KafkaMetric metric) {
    MetricName metricName = metric.metricName();
    recordedMetrics.remove(metricName);
    LOGGER.debug("Removed metric {}", createMetricName(metricName));
  }

  @Override
  public void close() {
    // no-op
  }

  @Override
  public void configure(Map<String, ?> configs) {
    this.isSendAll = ofNullable(configs.get(METRICS_SEND_ALL)).map(value -> Boolean.parseBoolean(value.toString())).orElse(false);
    String metrics = ofNullable(configs.get(METRICS_ALLOWED)).map(Object::toString).orElse("");
    this.allowedMetrics = new HashSet<>();
    if (!metrics.isEmpty() && !metrics.isBlank()) {
      String[] rawMetricNames = metrics.split(",");
      for (String rawMetricName : rawMetricNames) {
        this.allowedMetrics.add(rawMetricName.trim());
      }
    }
    this.allowedMetrics.addAll(CRITICAL_METRICS);

    this.serviceName = ofNullable(configs.get(SERVICE_NAME)).map(Object::toString).orElseThrow();
    // A workaround to support a single instance of StatsD client, see ru.hh.nab.kafka.util.ConfigProvider
    this.statsDSender = (StatsDSender) configs.get(STATSD_INSTANCE_PROPERTY);
  }

  private void recordMetric(KafkaMetric metric) {
    String name = createMetricName(metric.metricName());
    if (isSendAll || allowedMetrics.contains(name)) {
      recordedMetrics.put(metric.metricName(), metric);
      LOGGER.debug("Added metric {}", name);
    }
  }

  public enum ReporterTag {
    /**
     * When submitting tags to Okmeter through StatsD use underscore '_'
     * because okmeter doesn't comply to DataDog StatsD standards
     * https://docs.datadoghq.com/getting_started/tagging/
     */
    NODE_ID("node-id", "node_id"),
    CLIENT_ID("client-id", "client_id"),
    PARTITION("partition", "partition"),
    TOPIC("topic", "topic");

    private final String kafkaTag;
    private final String statsDTag;

    ReporterTag(String kafkaTag, String statsDTag) {
      this.kafkaTag = kafkaTag;
      this.statsDTag = statsDTag;
    }

    public String getKafkaTag() {
      return kafkaTag;
    }

    public String getStatsDTag() {
      return statsDTag;
    }
  }
}
