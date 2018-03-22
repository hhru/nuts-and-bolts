package ru.hh.nab.hibernate;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.metrics.StatsDSender;
import ru.hh.nab.common.util.PropertiesUtils;

import java.util.Properties;

@Configuration
@Import({HibernateCommonConfig.class})
public class HibernateProdConfig {

  @Bean
  Properties hibernateProperties() throws Exception {
    return PropertiesUtils.fromFilesInSettingsDir("hibernate.properties", "hibernate.properties.dev");
  }

  @Bean
  DataSourceFactory dataSourceFactory(String serviceName, StatsDSender statsDSender) {
    return new DataSourceFactory(serviceName, statsDSender);
  }
}
