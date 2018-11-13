package ru.hh.nab.testbase;

import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.web.context.WebApplicationContext;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.starter.NabCommonConfig;

import java.util.Properties;
import static ru.hh.nab.starter.server.jetty.JettyServerFactory.createJettyThreadPool;

@Configuration
@Import({NabCommonConfig.class})
public class NabTestConfig {
  public static final String TEST_SERVICE_NAME = "testService";

  @Bean
  Properties serviceProperties() {
    Properties properties = new Properties();
    properties.setProperty("jetty.port", "0");
    properties.setProperty("jetty.maxThreads", "8");
    properties.setProperty("serviceName", TEST_SERVICE_NAME);
    properties.setProperty("customTestProperty", "testValue");
    return properties;
  }


  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  ThreadPool jettyThreadPool(FileSettings fileSettings) throws Exception {
    return createJettyThreadPool(fileSettings.getSubSettings("jetty"));
  }

  @Bean
  StatsDClient statsDClient() {
    return new NoOpStatsDClient();
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  JettyTestContainer jettyTestContainer(WebApplicationContext context) {
    NabTestBase testInstance = NabTestBase.NabRunner.get();
    JettyTestContainerFactory factory = new JettyTestContainerFactory(context, testInstance.getApplication(), testInstance.getClass());
    return factory.createTestContainer();
  }
}
