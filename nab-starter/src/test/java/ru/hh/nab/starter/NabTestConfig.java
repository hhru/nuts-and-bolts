package ru.hh.nab.starter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.common.properties.FileSettings;

import java.util.Properties;

@Configuration
@Import({NabCommonConfig.class})
public class NabTestConfig {
  static final String TEST_SERVICE_NAME = "testService";
  static final String TEST_DATACENTER = "testDC";

  @Bean
  FileSettings fileSettings() {
    Properties properties = new Properties();
    properties.setProperty("jetty.port", "0");
    properties.setProperty("serviceName", TEST_SERVICE_NAME);
    properties.setProperty("datacenter", TEST_DATACENTER);
    return new FileSettings(properties);
  }
}
