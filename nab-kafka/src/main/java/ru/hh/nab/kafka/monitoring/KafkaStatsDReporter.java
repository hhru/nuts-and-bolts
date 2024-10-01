package ru.hh.nab.kafka.monitoring;

import java.util.List;
import java.util.Map;
import static java.util.Optional.ofNullable;
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
  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaStatsDReporter.class);
  public static final String STATSD_INSTANCE_PROPERTY = "NAB_STATSD_INSTANCE";

  private String serviceName;
  private StatsDSender statsDSender;

  protected final ConcurrentMap<MetricName, Metric> recordedMetrics = new ConcurrentHashMap<>();

  @Override
  public void init(List<KafkaMetric> metrics) {
    for (KafkaMetric metric : metrics) {
      MetricName metricName = metric.metricName();
      recordedMetrics.put(metricName, metric);
      LOGGER.debug("Added metric %s on initialization step".formatted(createMetricName(metricName)));
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
            LOGGER.debug("Sent gauge value %s for metric %s".formatted(value.toString(), name));
          } else {
            statsDSender.sendSetValue(name, metricValue.toString(), serviceNameTag, nodeIdTag, clientIdTag);
            LOGGER.debug("Sent set value %s for metric %s".formatted(value.toString(), name));
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
    MetricName metricName = metric.metricName();
    recordedMetrics.put(metricName, metric);
    LOGGER.debug("Added metric %s".formatted(createMetricName(metricName)));
  }

  @Override
  public void metricRemoval(KafkaMetric metric) {
    MetricName metricName = metric.metricName();
    recordedMetrics.remove(metricName);
    LOGGER.debug("Removed metric %s".formatted(createMetricName(metricName)));
  }

  @Override
  public void close() {
    // no-op
  }

  @Override
  public void configure(Map<String, ?> configs) {
    this.serviceName = ofNullable(configs.get(SERVICE_NAME)).map(Object::toString).orElseThrow();
    // A workaround to support a single instance of StatsD client, see ru.hh.nab.kafka.util.ConfigProvider
    this.statsDSender = (StatsDSender) configs.get(STATSD_INSTANCE_PROPERTY);
  }

  private static String createMetricName(MetricName metricName) {
    return String.format("%s.%s", metricName.group(), metricName.name());
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

    ReporterTag(String kafkaTag, String statsDTag) {
      this.kafkaTag = kafkaTag;
      this.statsDTag = statsDTag;
    }

    private final String kafkaTag;
    private final String statsDTag;

    public String getKafkaTag() {
      return kafkaTag;
    }

    public String getStatsDTag() {
      return statsDTag;
    }
  }
}
