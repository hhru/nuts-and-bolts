package ru.hh.nab.testbase;

import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.StatsDClient;
import java.util.Properties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource({
    "classpath:application-testbase.properties",
    "classpath:hibernate-testbase.properties",
    "classpath:kafka-testbase.properties"
})
public class NabTestConfig {

  public static final String TEST_SERVICE_NAME = "testService";
  public static final String TEST_SERVICE_VERSION = "test-version";

  @Bean
  public BuildProperties buildProperties() {
    Properties properties = new Properties();
    properties.setProperty("version", TEST_SERVICE_VERSION);
    return new BuildProperties(properties);
  }

  @Bean
  StatsDClient statsDClient() {
    return new NoOpStatsDClient();
  }
}
