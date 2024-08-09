package ru.hh.nab.starter;

import com.timgroup.statsd.StatsDClient;
import jakarta.inject.Named;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import ru.hh.nab.common.executor.ScheduledExecutor;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.qualifier.NamedQualifier.DATACENTER;
import static ru.hh.nab.common.qualifier.NamedQualifier.NODE_NAME;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import static ru.hh.nab.metrics.StatsDConstants.STATSD_DEFAULT_PERIODIC_SEND_INTERVAL;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.starter.metrics.JvmMetricsSender;
import ru.hh.nab.starter.qualifier.Service;
import static ru.hh.nab.starter.server.jetty.JettyServerFactory.createJettyThreadPool;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.JETTY;
import ru.hh.nab.starter.server.jetty.MonitoredQueuedThreadPool;

@Configuration
public class NabCommonConfig {
  private static final String NODE_NAME_ENV = "NODE_NAME";

  public static final String TEST_PROPERTIES_FILE_NAME = "service-test.properties";

  @Named(SERVICE_NAME)
  @Bean(SERVICE_NAME)
  String serviceName(FileSettings fileSettings) {
    return ofNullable(fileSettings.getString(SERVICE_NAME))
        .filter(Predicate.not(String::isEmpty))
        .orElseThrow(() -> new RuntimeException(String.format("'%s' property is not found in file settings", SERVICE_NAME)));
  }

  @Named(DATACENTER)
  @Bean(DATACENTER)
  String datacenter(FileSettings fileSettings) {
    return ofNullable(fileSettings.getString(DATACENTER))
        .filter(Predicate.not(String::isEmpty))
        .orElseThrow(() -> new RuntimeException(String.format("'%s' property is not found in file settings", DATACENTER)));
  }

  @Named(NODE_NAME)
  @Bean(NODE_NAME)
  String nodeName(FileSettings fileSettings) {
    return ofNullable(System.getenv(NODE_NAME_ENV))
        .orElseGet(
            () -> ofNullable(fileSettings.getString(NODE_NAME))
                .filter(Predicate.not(String::isEmpty))
                .orElseThrow(() -> new RuntimeException(String.format("'%s' property is not found in file settings", NODE_NAME)))
        );
  }

  @Bean
  MonitoredQueuedThreadPool jettyThreadPool(
      FileSettings fileSettings,
      @Named(SERVICE_NAME) String serviceNameValue,
      StatsDSender statsDSender
  ) throws Exception {
    return createJettyThreadPool(fileSettings.getSubSettings(JETTY), serviceNameValue, statsDSender);
  }

  @Bean
  FileSettings fileSettings(@Service Properties serviceProperties) {
    return new FileSettings(serviceProperties);
  }

  @Bean
  ScheduledExecutorService scheduledExecutorService() {
    return new ScheduledExecutor();
  }

  @Bean
  StatsDSender statsDSender(
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
