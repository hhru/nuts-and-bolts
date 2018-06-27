package ru.hh.nab.core;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.metrics.StatsDSender;
import ru.hh.nab.common.properties.FileSettings;

import java.util.Properties;

@Configuration
@Import({CoreCommonConfig.class})
public class CoreTestConfig {
  public static final int TEST_PORT = 8888;
  public static final String TEST_SERVICE_NAME = "testService";

  @Bean
  FileSettings fileSettings() {
    Properties properties = new Properties();
    properties.setProperty("jetty.port", String.valueOf(TEST_PORT));
    properties.setProperty("serviceName", TEST_SERVICE_NAME);
    return new FileSettings(properties);
  }

  @Bean
  StatsDSender statsDSender() {
    return Mockito.mock(StatsDSender.class);
  }
}
