package ru.hh.nab.core;

import static java.util.Optional.ofNullable;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.nab.common.executor.ScheduledExecutor;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.core.jetty.JettyFactory.createJettyThreadPool;

import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class CoreCommonConfig {
  private static final String SERVICE_NAME_PROPERTY = "serviceName";

  @Bean
  String serviceName(FileSettings fileSettings) {
    return ofNullable(fileSettings.getString(SERVICE_NAME_PROPERTY))
        .orElseThrow(() -> new RuntimeException(String.format("'%s' property is not found in file settings", SERVICE_NAME_PROPERTY)));
  }

  @Bean
  ThreadPool jettyThreadPool(FileSettings fileSettings) throws Exception {
    return createJettyThreadPool(fileSettings.getSubSettings("jetty"));
  }

  @Bean
  ScheduledExecutorService scheduledExecutorService() {
    return new ScheduledExecutor();
  }

  @Bean
  AppMetadata appMetadata(String serviceName) {
    return new AppMetadata(serviceName);
  }
}
