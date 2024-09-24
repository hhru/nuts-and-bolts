package ru.hh.nab.web;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClientBuilder;
import com.timgroup.statsd.StatsDClient;
import jakarta.inject.Named;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
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
import ru.hh.nab.profile.MainProfile;
import ru.hh.nab.starter.metrics.JvmMetricsSender;

@Configuration
public class NabMetricsConfiguration {

  @Bean
  public StatsDSender statsDSender(
      ScheduledExecutorService scheduledExecutorService,
      StatsDClient statsDClient,
      @Named(SERVICE_NAME) String serviceNameValue,
      FileSettings fileSettings
  ) {
    StatsDSender statsDSender = Optional
        .ofNullable(fileSettings.getInteger(STATSD_DEFAULT_PERIODIC_SEND_INTERVAL))
        .map(defaultPeriodicSendInterval -> new StatsDSender(statsDClient, scheduledExecutorService, defaultPeriodicSendInterval))
        .orElseGet(() -> new StatsDSender(statsDClient, scheduledExecutorService));
    if (Boolean.TRUE.equals(fileSettings.getBoolean("metrics.jvm.enabled"))) {
      JvmMetricsSender.create(statsDSender, serviceNameValue);
    }
    return statsDSender;
  }

  @Bean
  @MainProfile
  public StatsDClient statsDClient(FileSettings fileSettings) {

    String host = ofNullable(fileSettings.getString(STATSD_HOST_PROPERTY))
        .or(() -> ofNullable(System.getProperty(STATSD_HOST_ENV)))
        .orElse("localhost");

    int port = ofNullable(fileSettings.getString(STATSD_PORT_PROPERTY))
        .or(() -> ofNullable(System.getProperty(STATSD_PORT_ENV)))
        .map(Integer::parseInt)
        .orElse(8125);

    int queueSize = ofNullable(fileSettings.getString(STATSD_QUEUE_SIZE_PROPERTY))
        .or(() -> ofNullable(System.getProperty(STATSD_QUEUE_SIZE_ENV)))
        .map(Integer::parseInt)
        .orElse(10_000);

    int maxPacketSizeBytes = ofNullable(fileSettings.getString(STATSD_MAX_PACKET_SIZE_BYTES_PROPERTY))
        .or(() -> ofNullable(System.getProperty(STATSD_MAX_PACKET_SIZE_BYTES_ENV)))
        .map(Integer::parseInt)
        .orElse(NonBlockingStatsDClient.DEFAULT_UDP_MAX_PACKET_SIZE_BYTES);

    int bufferPoolSize = ofNullable(fileSettings.getString(STATSD_BUFFER_POOL_SIZE_PROPERTY))
        .or(() -> ofNullable(System.getProperty(STATSD_BUFFER_POOL_SIZE_ENV)))
        .map(Integer::parseInt)
        .orElse(8);

    return new NonBlockingStatsDClientBuilder()
        .hostname(host)
        .queueSize(queueSize)
        .port(port)
        .enableAggregation(false)
        .originDetectionEnabled(false)
        .enableTelemetry(false)
        .maxPacketSizeBytes(maxPacketSizeBytes)
        .bufferPoolSize(bufferPoolSize)
        .build();
  }
}
