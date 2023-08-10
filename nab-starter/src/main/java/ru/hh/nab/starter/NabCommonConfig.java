package ru.hh.nab.starter;

import com.timgroup.statsd.StatsDClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import ru.hh.nab.common.executor.ScheduledExecutor;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.common.qualifier.NamedQualifier;
import static ru.hh.nab.common.qualifier.NamedQualifier.COMMON_SCHEDULED_EXECUTOR;
import static ru.hh.nab.common.qualifier.NamedQualifier.DATACENTER;
import static ru.hh.nab.common.qualifier.NamedQualifier.NODE_NAME;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.starter.metrics.JvmMetricsSender;
import ru.hh.nab.starter.qualifier.Service;
import static ru.hh.nab.starter.server.jetty.JettyServerFactory.createJettyThreadPool;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.JETTY;
import ru.hh.nab.starter.server.jetty.MonitoredQueuedThreadPool;

@ApplicationScoped
public class NabCommonConfig {

  private static final String STATSD_DEFAULT_PERIODIC_SEND_INTERVAL = "statsd.defaultPeriodicSendIntervalSec";
  private static final String NODE_NAME_ENV = "NODE_NAME";

  public static final String TEST_PROPERTIES_FILE_NAME = "service-test.properties";

  @Produces
  @Singleton
  @Named(SERVICE_NAME)
  String serviceName(FileSettings fileSettings) {
    return ofNullable(fileSettings.getString(SERVICE_NAME)).filter(Predicate.not(String::isEmpty))
        .orElseThrow(() -> new RuntimeException(String.format("'%s' property is not found in file settings", SERVICE_NAME)));
  }

  @Produces
  @Singleton
  @Named(DATACENTER)
  String datacenter(FileSettings fileSettings) {
    return ofNullable(fileSettings.getString(DATACENTER)).filter(Predicate.not(String::isEmpty))
        .orElseThrow(() -> new RuntimeException(String.format("'%s' property is not found in file settings", DATACENTER)));
  }

  @Produces
  @Singleton
  @Named(NODE_NAME)
  String nodeName(FileSettings fileSettings) {
    return ofNullable(System.getenv(NODE_NAME_ENV))
        .orElseGet(() -> ofNullable(fileSettings.getString(NODE_NAME)).filter(Predicate.not(String::isEmpty))
            .orElseThrow(() -> new RuntimeException(String.format("'%s' property is not found in file settings", NODE_NAME))));
  }

  @Produces
  @Singleton
  MonitoredQueuedThreadPool jettyThreadPool(
      FileSettings fileSettings,
      @Named(SERVICE_NAME) String serviceNameValue,
      StatsDSender statsDSender
  ) throws Exception {
    return createJettyThreadPool(fileSettings.getSubSettings(JETTY), serviceNameValue, statsDSender);
  }

  @Produces
  @Singleton
  FileSettings fileSettings(@Service Properties serviceProperties) {
    return new FileSettings(serviceProperties);
  }

  @Produces
  @Singleton
  @Named(COMMON_SCHEDULED_EXECUTOR)
  ScheduledExecutorService scheduledExecutorService() {
    return new ScheduledExecutor();
  }

  @Produces
  @Singleton
  StatsDSender statsDSender(
      @Named(COMMON_SCHEDULED_EXECUTOR) ScheduledExecutorService scheduledExecutorService,
      StatsDClient statsDClient,
      @Named(SERVICE_NAME) String serviceNameValue,
      FileSettings fileSettings
  ) {
    StatsDSender statsDSender = Optional.ofNullable(fileSettings.getInteger(STATSD_DEFAULT_PERIODIC_SEND_INTERVAL))
        .map(defaultPeriodicSendInterval -> new StatsDSender(statsDClient, scheduledExecutorService, defaultPeriodicSendInterval))
        .orElseGet(() -> new StatsDSender(statsDClient, scheduledExecutorService));
    if (Boolean.TRUE.equals(fileSettings.getBoolean("metrics.jvm.enabled"))) {
      JvmMetricsSender.create(statsDSender, serviceNameValue);
    }
    return statsDSender;
  }

  @Produces
  @Singleton
  @Named(NamedQualifier.PROJECT_PROPERTIES)
  Properties projectProperties() {
    Properties properties = new Properties();
    try (InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream(AppMetadata.PROJECT_PROPERTIES)) {
      if (resource != null) {
        properties.load(resource);
      }
    } catch (IOException e) {
      throw new RuntimeException("Couldn't load project properties", e);
    }
    return properties;
  }

  @Produces
  @Singleton
  AppMetadata appMetadata(@Named(SERVICE_NAME) String serviceName, @Named(NamedQualifier.PROJECT_PROPERTIES) Properties projectProperties) {
    return new AppMetadata(serviceName, projectProperties);
  }
}
