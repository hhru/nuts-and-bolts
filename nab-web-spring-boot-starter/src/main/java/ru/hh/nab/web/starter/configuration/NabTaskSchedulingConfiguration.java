package ru.hh.nab.web.starter.configuration;

import java.util.concurrent.ScheduledExecutorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.nab.common.executor.ScheduledExecutor;

@Configuration
public class NabTaskSchedulingConfiguration {

  @Bean
  public ScheduledExecutorService scheduledExecutorService() {
    return new ScheduledExecutor();
  }
}
