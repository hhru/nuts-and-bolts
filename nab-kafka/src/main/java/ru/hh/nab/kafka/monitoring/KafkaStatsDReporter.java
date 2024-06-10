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
import org.apache.kafka.clients.consumer.ConsumerConfig;
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
  private static final String CLIENT_ID_TAG_NAME = ConsumerConfig.CLIENT_ID_CONFIG.replace(".", "-");
  private static final String TOPIC_TAG_NAME = "topic";

  private String serviceName;
  private StatsDClient statsDClient;
  private final Map<String, Object> statsdClientProperties = new LinkedHashMap<>();

  private StatsDSender statsDSender;
  private ScheduledExecutorService scheduledExecutorService;
  private final Map<String, Object> statsdSenderProperties = new LinkedHashMap<>();

  private final ConcurrentMap<String, Metric> recordedMetrics = new ConcurrentHashMap<>();

  @Override
  public void init(List<KafkaMetric> metrics) {
    statsDSender.sendPeriodically(() -> {
      recordedMetrics.forEach((key, value) -> {
        Object metricValue = value.metricValue();
        MetricName metricName = value.metricName();
        Map<String, String> tags = metricName.tags();

        String clientId = tags.getOrDefault(CLIENT_ID_TAG_NAME, "unknown-client-id");
        Tag clientIdTag = new Tag(CLIENT_ID_TAG_NAME, clientId);

        Tag serviceNameTag = new Tag(Tag.APP_TAG_NAME, this.serviceName);
        String name = createMetricName(metricName);
        if (metricValue instanceof Number number) {
          String topic = tags.getOrDefault(TOPIC_TAG_NAME, "unknown-topic");
          Tag topicTag = new Tag(TOPIC_TAG_NAME, topic);

          statsDSender.sendGauge(name, number.doubleValue(), serviceNameTag, clientIdTag, topicTag);
          LOGGER.debug("Sent gauge value %s for metric %s".formatted(value.toString(), name));
        } else {
          statsDSender.sendSetValue(name, metricValue.toString(), serviceNameTag, clientIdTag);
          LOGGER.debug("Sent set value %s for metric %s".formatted(value.toString(), name));
        }
      });
      recordedMetrics.clear();
    });
  }

  @Override
  public void metricChange(KafkaMetric metric) {
    Object value = metric.metricValue();
    if (value == null) {
      return;
    }

    String metricName = createMetricName(metric.metricName());
    recordedMetrics.put(metricName, metric);
    LOGGER.debug("Recorded value %s for metric %s".formatted(value.toString(), metricName));
  }

  @Override
  public void metricRemoval(KafkaMetric metric) {
    // no-ops case
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
}
