package ru.hh.nab.testbase;

import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.web.NabDeployInfoConfiguration;
import ru.hh.nab.web.NabMetricsConfiguration;
import ru.hh.nab.web.NabTaskSchedulingConfiguration;

@Configuration
@Import({
    NabProjectInfoConfiguration.class,
    NabDeployInfoConfiguration.class,
    NabMetricsConfiguration.class,
    NabTaskSchedulingConfiguration.class,
})
public class NabTestConfig {
  public static final String TEST_SERVICE_NAME = "testService";

  @Bean
  StatsDClient statsDClient() {
    return new NoOpStatsDClient();
  }
}
