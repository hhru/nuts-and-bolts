package ru.hh.nab.starter;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import static java.util.Optional.ofNullable;
import java.util.concurrent.ScheduledExecutorService;
import javax.management.MBeanServer;
import org.eclipse.jetty.servlet.FilterHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import ru.hh.metrics.StatsDSender;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.properties.PropertiesUtils.fromFilesInSettingsDir;
import ru.hh.nab.starter.jmx.MBeanExporterFactory;
import static ru.hh.nab.starter.server.cache.HttpCacheFilterFactory.createCacheFilterHolder;

@Configuration
@Import({NabCommonConfig.class})
public class NabProdConfig {
  private static final String PROPERTIES_FILE_NAME = "service.properties";
  private static final String DATACENTER_NAME_PROPERTY = "datacenter";

  @Bean
  FileSettings fileSettings() throws Exception {
    return new FileSettings(fromFilesInSettingsDir(PROPERTIES_FILE_NAME));
  }

  @Bean
  String datacenter(FileSettings fileSettings) {
    return ofNullable(fileSettings.getString(DATACENTER_NAME_PROPERTY))
        .orElseThrow(() -> new RuntimeException(String.format("'%s' property is not found in file settings", DATACENTER_NAME_PROPERTY)));
  }

  @Bean
  StatsDClient statsDClient() {
    return new NonBlockingStatsDClient(null, "localhost", 8125, 10000);
  }

  @Bean
  StatsDSender statsDSender(ScheduledExecutorService scheduledExecutorService, StatsDClient statsDClient) {
    return new StatsDSender(statsDClient, scheduledExecutorService);
  }

  @Bean
  FilterHolder cacheFilter(FileSettings fileSettings,
                           String serviceName,
                           StatsDClient statsDClient,
                           ScheduledExecutorService scheduledExecutorService) {
    return createCacheFilterHolder(fileSettings, serviceName, statsDClient, scheduledExecutorService);
  }

  @Bean
  MBeanServerFactoryBean mBeanServerFactoryBean() {
    MBeanServerFactoryBean mBeanServerFactoryBean = new MBeanServerFactoryBean();
    mBeanServerFactoryBean.setLocateExistingServerIfPossible(true);
    return mBeanServerFactoryBean;
  }

  @Bean
  MBeanExporter mBeanExporter(FileSettings settings, MBeanServer mbeanServer) {
    return MBeanExporterFactory.create(settings, mbeanServer);
  }
}
