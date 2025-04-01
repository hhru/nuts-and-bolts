package ru.hh.nab.jclient.metrics;

import io.netty.util.internal.StringUtil;
import java.util.Map;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import static java.util.stream.Collectors.toMap;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.hh.jclient.common.Monitoring;

public class KafkaUpstreamMonitoring implements Monitoring {
  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaUpstreamMonitoring.class);
  private static final String ENABLED_PROPERTY_KEY = "enabled";
  private static final String REQUEST_TOPIC_KEY = "topics.requests";
  private static final Set<String> PROPERTIES_FOR_MONITORING = Set.of(ENABLED_PROPERTY_KEY, REQUEST_TOPIC_KEY);

  private final String serviceName;
  private final String localDc;
  private final KafkaProducer<String, String> kafkaProducer;
  private final Config monitoringConfig;

  public KafkaUpstreamMonitoring(
      String serviceName,
      String localDc,
      KafkaProducer<String, String> kafkaMetricsProducer,
      Config monitoringConfig
  ) {
    this.serviceName = serviceName;
    this.localDc = localDc;
    this.kafkaProducer = kafkaMetricsProducer;
    this.monitoringConfig = monitoringConfig;
  }

  @Override
  public void countRequest(String upstreamName, String dc, String hostname, int statusCode, long requestTimeMillis, boolean isRequestFinal) {
    if (isRequestFinal) {
      var requestId = ofNullable(MDC.get("rid")).orElse("");
      var jsonBuilder = new SimpleJsonBuilder();
      jsonBuilder.put("ts", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
      jsonBuilder.put("app", serviceName);
      jsonBuilder.put("upstream", upstreamName);
      jsonBuilder.put("dc", StringUtil.isNullOrEmpty(dc) ? localDc : dc);
      jsonBuilder.put("hostname", hostname);
      jsonBuilder.put("status", statusCode);
      jsonBuilder.put("requestId", requestId);
      String json = jsonBuilder.build();
      LOGGER.debug("Sending countRequest with json {}", json);
      ProducerRecord<String, String> record = new ProducerRecord<>(monitoringConfig.requestsCountTopicName, json);
      kafkaProducer.send(record, (recordMetadata, e) -> {
        if (e != null) {
          LOGGER.warn(e.getMessage(), e);
        }
      });
    }
  }

  @Override
  public void countRequestTime(String upstreamName, String dc, long requestTimeMillis) {

  }

  @Override
  public void countRetry(String upstreamName, String dc, String serverAddress, int statusCode, int firstStatusCode, int triesUsed) {

  }

  @Override
  public void countUpdateIgnore(String upstreamName, String serverDatacenter) {

  }

  public static Optional<KafkaUpstreamMonitoring> fromProperties(String serviceName, String dc, Properties properties) {
    return ofNullable(properties)
        .map(props -> props.getProperty(ENABLED_PROPERTY_KEY))
        .map(Boolean::parseBoolean)
        .filter(Boolean.TRUE::equals)
        .map(ignored -> {
          Map<?, ?> propertiesForKafka = properties
              .entrySet()
              .stream()
              .filter(entry -> !PROPERTIES_FOR_MONITORING.contains(entry.getKey()))
              .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
          var filteredProperties = new Properties();
          filteredProperties.putAll(propertiesForKafka);
          return new KafkaProducer<>(filteredProperties, new StringSerializer(), new StringSerializer());
        })
        .map(producer -> new KafkaUpstreamMonitoring(serviceName, dc, producer, Config.fromProperties(properties)));
  }

  static class Config {
    final String requestsCountTopicName;

    Config(String requestsCountTopicName) {
      this.requestsCountTopicName = requestsCountTopicName;
    }

    static Config fromProperties(Properties properties) {
      var requestsCountTopicName = properties.getProperty(REQUEST_TOPIC_KEY);
      return new Config(requestsCountTopicName);
    }
  }
}
