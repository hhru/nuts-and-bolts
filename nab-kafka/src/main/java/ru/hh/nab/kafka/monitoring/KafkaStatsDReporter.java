package ru.hh.nab.kafka.monitoring;

import com.timgroup.statsd.NonBlockingStatsDClientBuilder;
import com.timgroup.statsd.StatsDClient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static java.util.Optional.ofNullable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.MetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_BUFFER_POOL_SIZE_PROPERTY;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_DEFAULT_PERIODIC_SEND_INTERVAL;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_HOST_PROPERTY;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_MAX_PACKET_SIZE_BYTES_PROPERTY;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_PORT_PROPERTY;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_QUEUE_SIZE_PROPERTY;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.Tag;

public class KafkaStatsDReporter implements MetricsReporter {
  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaStatsDReporter.class);

  private String serviceName;

  private StatsDClient statsDClient;
  private final Map<String, Object> statsdClientProperties = new LinkedHashMap<>();
  private StatsDSender statsDSender;

  private ScheduledExecutorService scheduledExecutorService;
  private final Map<String, Object> statsdSenderProperties = new LinkedHashMap<>();
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
    int sendInterval = Integer.parseInt(statsdSenderProperties.get(STATSD_DEFAULT_PERIODIC_SEND_INTERVAL).toString());
    // allowing any stored metrics to be sent
    this.scheduledExecutorService.shutdown();
    try {
      if (!this.scheduledExecutorService.awaitTermination(sendInterval, TimeUnit.SECONDS)) {
        this.scheduledExecutorService.shutdownNow();
        if (!this.scheduledExecutorService.awaitTermination(sendInterval, TimeUnit.SECONDS)) {
          LOGGER.error("Failed to shutdown thread pool for statsd metrics sender");
        }
      }
    } catch (InterruptedException e) {
      // shutdown could be interrupted
      this.scheduledExecutorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
    this.statsDClient.close();
  }

  @Override
  public void configure(Map<String, ?> configs) {
    String host = configs.get(STATSD_HOST_PROPERTY).toString();
    statsdClientProperties.put(STATSD_HOST_PROPERTY, host);

    int port = Integer.parseInt(configs.get(STATSD_PORT_PROPERTY).toString());
    statsdClientProperties.put(STATSD_PORT_PROPERTY, port);

    int queueSize = Integer.parseInt(configs.get(STATSD_QUEUE_SIZE_PROPERTY).toString());
    statsdClientProperties.put(STATSD_QUEUE_SIZE_PROPERTY, queueSize);

    int bufferPoolSize = Integer.parseInt(configs.get(STATSD_BUFFER_POOL_SIZE_PROPERTY).toString());
    statsdClientProperties.put(STATSD_BUFFER_POOL_SIZE_PROPERTY, bufferPoolSize);

    int maxPacketSizeBytes = Integer.parseInt(configs.get(STATSD_MAX_PACKET_SIZE_BYTES_PROPERTY).toString());
    statsdClientProperties.put(STATSD_MAX_PACKET_SIZE_BYTES_PROPERTY, maxPacketSizeBytes);

    this.statsDClient = new NonBlockingStatsDClientBuilder()
        .hostname(host)
        .queueSize(queueSize)
        .port(port)
        .enableAggregation(false)
        .originDetectionEnabled(false)
        .enableTelemetry(false)
        .maxPacketSizeBytes(maxPacketSizeBytes)
        .bufferPoolSize(bufferPoolSize)
        .build();

    this.serviceName = ofNullable(configs.get(SERVICE_NAME)).map(Object::toString).orElseThrow();

    String statsdClientSummary = createPropertiesSummary(statsdClientProperties);
    LOGGER.info("StatsD client values : %s".formatted(statsdClientSummary));

    int sendIntervalSeconds = Integer.parseInt(configs.get(STATSD_DEFAULT_PERIODIC_SEND_INTERVAL).toString());
    this.statsdSenderProperties.put(STATSD_DEFAULT_PERIODIC_SEND_INTERVAL, sendIntervalSeconds);

    this.scheduledExecutorService = Executors.newScheduledThreadPool(1);
    this.statsDSender = new StatsDSender(this.statsDClient, this.scheduledExecutorService, sendIntervalSeconds);

    String statsdSenderSummary = createPropertiesSummary(statsdSenderProperties);
    LOGGER.info("StatsD sender values : %s".formatted(statsdSenderSummary));
  }

  private static String createPropertiesSummary(Map<String, Object> properties) {
    return properties
        .entrySet()
        .stream()
        .map((entry) -> "\n\t%s = %s".formatted(entry.getKey(), entry.getValue()))
        .collect(Collectors.joining("", "", "\n"));
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
