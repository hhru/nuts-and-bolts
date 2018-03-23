package ru.hh.nab.core;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.metrics.StatsDSender;
import ru.hh.nab.common.util.FileSettings;
import ru.hh.nab.core.util.HhScheduledExecutor;

import java.util.concurrent.ScheduledExecutorService;

import static ru.hh.nab.core.jetty.JettyFactory.createJettyThreadPool;

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
  String stackOuterClassExcluding() {
    return ServletContainer.class.getName();
  }

  @Bean
  ScheduledExecutorService scheduledExecutorService() {
    return new HhScheduledExecutor();
  }

  @Bean
  StatsDClient statsDClient() {
    return new NonBlockingStatsDClient(null, "localhost", 8125, 10000);
  }

  @Bean
  StatsDSender statsDSender(ScheduledExecutorService scheduledExecutorService, StatsDClient statsDClient) {
    return new StatsDSender(statsDClient, scheduledExecutorService);
  }

  @Bean
  AppMetadata appMetadata(String serviceName) {
    return new AppMetadata(serviceName);
  }
}
