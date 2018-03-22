package ru.hh.nab.core;

import com.timgroup.statsd.StatsDClient;
import org.eclipse.jetty.servlet.FilterHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import ru.hh.nab.core.jmx.MBeanExporterFactory;
import ru.hh.nab.common.util.FileSettings;

import javax.management.MBeanServer;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

import static ru.hh.nab.core.jetty.HttpCacheFilterFactory.createCacheFilterHolder;
import static ru.hh.nab.common.util.PropertiesUtils.fromFilesInSettingsDir;

@Configuration
@Import({CoreCommonConfig.class})
public class CoreProdConfig {

  @Bean
  FileSettings fileSettings() throws Exception {
    Properties properties = fromFilesInSettingsDir("service.properties", "service.properties.dev");
    return new FileSettings(properties);
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

  @Bean
  StatusResource statusResource(AppMetadata appMetadata) {
    return new StatusResource(appMetadata);
  }

  @Bean
  StatsResource statsResource() {
    return new StatsResource();
  }
}
