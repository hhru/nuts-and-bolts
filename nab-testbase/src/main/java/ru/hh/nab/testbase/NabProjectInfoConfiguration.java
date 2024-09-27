package ru.hh.nab.testbase;

import java.util.Properties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NabProjectInfoConfiguration {

  public static final String TEST_SERVICE_VERSION = "test-version";

  @Bean
  public BuildProperties buildProperties() {
    Properties properties = new Properties();
    properties.setProperty("version", TEST_SERVICE_VERSION);
    return new BuildProperties(properties);
  }
}
