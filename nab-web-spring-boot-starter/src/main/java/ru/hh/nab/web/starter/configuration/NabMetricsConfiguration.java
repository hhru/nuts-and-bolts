package ru.hh.nab.web.starter.configuration;

import com.timgroup.statsd.StatsDClient;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import ru.hh.nab.common.spring.boot.env.EnvironmentUtils;
import ru.hh.nab.common.spring.boot.profile.MainProfile;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.metrics.clients.JvmMetricsSender;
import ru.hh.nab.metrics.factory.StatsDClientFactory;
import ru.hh.nab.web.starter.configuration.properties.InfrastructureProperties;

@Configuration
public class NabMetricsConfiguration {

  public static final String METRICS_JVM_ENABLED_PROPERTY = "metrics.jvm.enabled";
  public static final String STATSD_DEFAULT_PERIODIC_SEND_INTERVAL_SEC_PROPERTY = "statsd.defaultPeriodicSendIntervalSec";

  @Bean
  public StatsDSender statsDSender(ScheduledExecutorService scheduledExecutorService, StatsDClient statsDClient, Environment environment) {
    return new StatsDSender(
        statsDClient,
        scheduledExecutorService,
        environment.getProperty(STATSD_DEFAULT_PERIODIC_SEND_INTERVAL_SEC_PROPERTY, Integer.class, StatsDSender.DEFAULT_SEND_INTERVAL_SECONDS)
    );
  }

  @Bean
  @ConditionalOnProperty(name = METRICS_JVM_ENABLED_PROPERTY, havingValue = "true")
  public JvmMetricsSender jvmMetricsSender(StatsDSender statsDSender, InfrastructureProperties infrastructureProperties) {
    return new JvmMetricsSender(statsDSender, infrastructureProperties.getServiceName());
  }

  @Bean
  @MainProfile
  public StatsDClient statsDClient(ConfigurableEnvironment environment) {
    return StatsDClientFactory.createNonBlockingClient(EnvironmentUtils.getProperties(environment));
  }
}
