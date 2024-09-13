package ru.hh.nab.starter;

import java.util.concurrent.ScheduledExecutorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.nab.common.executor.ScheduledExecutor;

@Configuration
public class NabCommonConfig {

  public static final String TEST_PROPERTIES_FILE_NAME = "service-test.properties";

  @Bean
  ScheduledExecutorService scheduledExecutorService() {
    return new ScheduledExecutor();
  }
}
