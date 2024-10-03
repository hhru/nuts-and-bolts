package ru.hh.nab.web;

import com.timgroup.statsd.StatsDClient;
import jakarta.inject.Named;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_DEFAULT_PERIODIC_SEND_INTERVAL;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.clients.JvmMetricsSender;
import ru.hh.nab.metrics.factory.StatsDClientFactory;
import ru.hh.nab.profile.MainProfile;

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
    return StatsDClientFactory.createNonBlockingClient(fileSettings.getAsMap());
  }
}
