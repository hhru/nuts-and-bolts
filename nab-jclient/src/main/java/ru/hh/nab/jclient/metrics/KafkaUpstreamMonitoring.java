package ru.hh.nab.jclient.metrics;

import io.netty.util.internal.StringUtil;
import static java.util.Optional.ofNullable;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.hh.jclient.common.Monitoring;
import ru.hh.nab.common.properties.PropertiesUtils;
import ru.hh.nab.kafka.producer.KafkaProducer;

public class KafkaUpstreamMonitoring implements Monitoring {
  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaUpstreamMonitoring.class);
  private static final String ENABLED_PROPERTY_KEY = "enabled";
  private static final String REQUEST_TOPIC_KEY = "topics.requests";
  private static final String DEFAULT_REQUEST_TOPIC = "metrics_requests";

  private final String serviceName;
  private final String localDc;
  private final KafkaProducer kafkaProducer;
  private final Executor kafkaExecutor;
  private final String topicName;
  private final boolean sendingEnabled;

  public KafkaUpstreamMonitoring(
      String serviceName,
      String localDc,
      KafkaProducer kafkaProducer,
      Executor kafkaExecutor,
      Properties properties
  ) {
    this.serviceName = serviceName;
    this.localDc = localDc;
    this.kafkaProducer = kafkaProducer;
    this.kafkaExecutor = kafkaExecutor;
    this.topicName = properties.getProperty(REQUEST_TOPIC_KEY, DEFAULT_REQUEST_TOPIC);
    this.sendingEnabled = PropertiesUtils.getBoolean(properties, ENABLED_PROPERTY_KEY, false);
  }

  @Override
  public void countRequest(
      String upstreamName,
      String serverDatacenter,
      String serverAddress,
      int statusCode,
      long requestTimeMillis,
      boolean isRequestFinal,
      String balancingStrategyType
  ) {
    if (sendingEnabled && isRequestFinal) {
      RequestInfo requestInfo = new RequestInfo(
          TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()),
          serviceName,
          upstreamName,
          StringUtil.isNullOrEmpty(serverDatacenter) ? localDc : serverDatacenter,
          serverAddress,
          statusCode,
          ofNullable(MDC.get("rid")).orElse("")
      );
      LOGGER.debug("Sending countRequest {}", requestInfo);
      kafkaProducer
          .sendMessage(topicName, requestInfo, kafkaExecutor)
          .whenComplete((result, e) -> {
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

  private record RequestInfo(long ts, String app, String upstream, String dc, String hostname, int status, String requestId) {

    @Override
    public String toString() {
      return "RequestInfo{" +
          "ts=" + ts +
          ", app='" + app + '\'' +
          ", upstream='" + upstream + '\'' +
          ", dc='" + dc + '\'' +
          ", hostname='" + hostname + '\'' +
          ", status=" + status +
          ", requestId='" + requestId + '\'' +
          '}';
    }
  }
}
