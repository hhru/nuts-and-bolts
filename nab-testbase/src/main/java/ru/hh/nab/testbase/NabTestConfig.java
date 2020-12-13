package ru.hh.nab.testbase;

import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.StatsDClient;
import java.io.IOException;
import java.util.Properties;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.metrics.StatsDSender;
import ru.hh.nab.starter.NabCommonConfig;
import static ru.hh.nab.starter.server.jetty.JettyServerFactory.createJettyThreadPool;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.JETTY;

@Configuration
@Import({NabCommonConfig.class})
public class NabTestConfig {
  public static final String TEST_SERVICE_NAME = "testService";
  static final String TEST_PROPERTIES_FILE_NAME = "service-test.properties";

  @Bean
  Properties serviceProperties() throws IOException {
    return createProperties(TEST_PROPERTIES_FILE_NAME);
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  ThreadPool jettyThreadPool(FileSettings fileSettings, String serviceName, StatsDSender statsDSender) throws Exception {
    return createJettyThreadPool(fileSettings.getSubSettings(JETTY), serviceName, statsDSender);
  }

  @Bean
  StatsDClient statsDClient() {
    return new NoOpStatsDClient();
  }

  public static Properties createProperties(String propertiesName) throws IOException {
    PropertiesFactoryBean properties = new PropertiesFactoryBean();
    properties.setSingleton(false);
    properties.setIgnoreResourceNotFound(true);
    properties.setLocations(new ClassPathResource(propertiesName));
    return properties.getObject();
  }
}
