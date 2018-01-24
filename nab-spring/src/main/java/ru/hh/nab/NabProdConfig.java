package ru.hh.nab;

import com.timgroup.statsd.StatsDClient;
import org.eclipse.jetty.servlet.FilterHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import ru.hh.nab.jmx.MBeanExporterFactory;
import ru.hh.nab.util.FileSettings;

import javax.management.MBeanServer;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

import static ru.hh.nab.jetty.HttpCacheFilterFactory.createCacheFilterHolder;
import static ru.hh.nab.util.PropertiesUtils.fromFilesInSettingsDir;

@Configuration
@Import({NabCommonConfig.class})
public class NabProdConfig {
  @Bean
  FileSettings fileSettings() throws Exception {
    Properties properties = fromFilesInSettingsDir("settings.properties", "settings.properties.dev");
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
}
