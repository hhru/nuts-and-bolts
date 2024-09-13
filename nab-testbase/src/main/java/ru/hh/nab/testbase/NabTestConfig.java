package ru.hh.nab.testbase;

import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.metrics.StatsDSender;
import static ru.hh.nab.starter.server.jetty.JettyServerFactory.createJettyThreadPool;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.JETTY;
import ru.hh.nab.web.NabDeployInfoConfiguration;
import ru.hh.nab.web.NabMetricsConfiguration;
import ru.hh.nab.web.NabTaskSchedulingConfiguration;

@Configuration
@Import({
    NabDeployInfoConfiguration.class,
    NabMetricsConfiguration.class,
    NabTaskSchedulingConfiguration.class,
})
public class NabTestConfig {
  public static final String TEST_SERVICE_NAME = "testService";

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  ThreadPool jettyThreadPool(FileSettings fileSettings, String serviceName, StatsDSender statsDSender) throws Exception {
    return createJettyThreadPool(fileSettings.getSubSettings(JETTY), serviceName, statsDSender);
  }

  @Bean
  StatsDClient statsDClient() {
    return new NoOpStatsDClient();
  }
}
