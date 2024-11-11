package ru.hh.nab.web.starter.configuration;

import com.timgroup.statsd.StatsDClient;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.nab.metrics.StatsDProperties;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.clients.JvmMetricsSender;
import ru.hh.nab.metrics.factory.StatsDClientFactory;
import ru.hh.nab.web.starter.configuration.properties.InfrastructureProperties;
import ru.hh.nab.web.starter.profile.MainProfile;

@Configuration
public class NabMetricsConfiguration {

  static final String METRICS_JVM_ENABLED_PROPERTY = "metrics.jvm.enabled";

  @Bean
  @ConfigurationProperties(StatsDProperties.PREFIX)
  public StatsDProperties statsDProperties() {
    return new StatsDProperties();
  }

  @Bean
  public StatsDSender statsDSender(ScheduledExecutorService scheduledExecutorService, StatsDClient statsDClient, StatsDProperties statsDProperties) {
    return new StatsDSender(statsDClient, scheduledExecutorService, statsDProperties.getDefaultPeriodicSendIntervalSec());
  }

  @Bean
  @ConditionalOnProperty(name = METRICS_JVM_ENABLED_PROPERTY, havingValue = "true")
  public JvmMetricsSender jvmMetricsSender(StatsDSender statsDSender, InfrastructureProperties infrastructureProperties) {
    return new JvmMetricsSender(statsDSender, infrastructureProperties.getServiceName());
  }

  @Bean
  @MainProfile
  public StatsDClient statsDClient(StatsDProperties statsDProperties) {
    return StatsDClientFactory.createNonBlockingClient(statsDProperties);
  }
}
