package ru.hh.nab.starter;

import jakarta.inject.Named;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.nab.common.executor.ScheduledExecutor;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.qualifier.NamedQualifier.SERVICE_NAME;
import ru.hh.nab.metrics.StatsDSender;
import static ru.hh.nab.starter.server.jetty.JettyServerFactory.createJettyThreadPool;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.JETTY;
import ru.hh.nab.starter.server.jetty.MonitoredQueuedThreadPool;

@Configuration
public class NabCommonConfig {

  public static final String TEST_PROPERTIES_FILE_NAME = "service-test.properties";

  @Bean
  MonitoredQueuedThreadPool jettyThreadPool(
      FileSettings fileSettings,
      @Named(SERVICE_NAME) String serviceNameValue,
      StatsDSender statsDSender
  ) throws Exception {
    return createJettyThreadPool(fileSettings.getSubSettings(JETTY), serviceNameValue, statsDSender);
  }

  @Bean
  ScheduledExecutorService scheduledExecutorService() {
    return new ScheduledExecutor();
  }
}
