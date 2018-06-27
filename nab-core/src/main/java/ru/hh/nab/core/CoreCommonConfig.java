package ru.hh.nab.core;

import org.eclipse.jetty.util.thread.ThreadPool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.nab.common.executor.ScheduledExecutor;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.core.jetty.JettyFactory.createJettyThreadPool;

import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class CoreCommonConfig {

  @Bean
  String serviceName(FileSettings fileSettings) {
    String serviceName = fileSettings.getString("serviceName");
    if (serviceName == null) {
      throw new RuntimeException("'serviceName' not found in file settings");
    }
    return serviceName;
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
