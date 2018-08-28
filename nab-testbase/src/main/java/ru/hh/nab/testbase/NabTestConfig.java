package ru.hh.nab.testbase;

import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.starter.NabCommonConfig;

import java.util.Properties;

@Configuration
@Import({NabCommonConfig.class})
public class NabTestConfig {
  public static final String TEST_SERVICE_NAME = "testService";

  @Bean
  FileSettings fileSettings() {
    Properties properties = new Properties();
    properties.setProperty("jetty.port", "0");
    properties.setProperty("jetty.maxThreads", "16");
    properties.setProperty("serviceName", TEST_SERVICE_NAME);
    return new FileSettings(properties);
  }

  @Bean
  StatsDClient statsDClient() {
    return new NoOpStatsDClient();
  }
}
