package ru.hh.nab.starter;

import static java.util.Optional.ofNullable;

import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

import com.timgroup.statsd.StatsDClient;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import ru.hh.nab.common.executor.ScheduledExecutor;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.starter.metrics.JvmMetricsSender;
import ru.hh.nab.starter.server.jetty.MonitoredQueuedThreadPool;

import static ru.hh.nab.starter.server.jetty.JettyServerFactory.createJettyThreadPool;

@Configuration
public class NabCommonConfig {
  static final String SERVICE_NAME_PROPERTY = "serviceName";

  @Bean
  String serviceName(FileSettings fileSettings) {
    return ofNullable(fileSettings.getString(SERVICE_NAME_PROPERTY))
        .orElseThrow(() -> new RuntimeException(String.format("'%s' property is not found in file settings", SERVICE_NAME_PROPERTY)));
  }

  @Bean
  MonitoredQueuedThreadPool jettyThreadPool(FileSettings fileSettings, String serviceName, StatsDSender statsDSender) throws Exception {
    return createJettyThreadPool(fileSettings.getSubSettings("jetty"), serviceName, statsDSender);
  }

  @Bean
  FileSettings fileSettings(Properties serviceProperties) {
    return new FileSettings(serviceProperties);
  }

  @Bean
  ScheduledExecutorService scheduledExecutorService() {
    return new ScheduledExecutor();
  }

  @Bean
  StatsDSender statsDSender(ScheduledExecutorService scheduledExecutorService, StatsDClient statsDClient, String serviceName,
                            FileSettings fileSettings) {
    StatsDSender statsDSender = new StatsDSender(statsDClient, scheduledExecutorService);
    if (Boolean.TRUE.equals(fileSettings.getBoolean("metrics.jvm.enabled"))) {
      JvmMetricsSender.create(statsDSender, serviceName);
    }
    return statsDSender;
  }

  @Bean
  PropertiesFactoryBean projectProperties() {
    PropertiesFactoryBean projectProps = new PropertiesFactoryBean();
    projectProps.setLocation(new ClassPathResource(AppMetadata.PROJECT_PROPERTIES));
    projectProps.setIgnoreResourceNotFound(true);
    return projectProps;
  }

  @Bean
  AppMetadata appMetadata(String serviceName, Properties projectProperties) {
    return new AppMetadata(serviceName, projectProperties);
  }
}
