package ru.hh.nab.hibernate;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.util.PropertiesUtils;

import java.util.Properties;

@Configuration
@Import({NabHibernateCommonConfig.class})
public class NabHibernateProdConfig {

  @Bean
  Properties hibernateProperties() throws Exception {
    return PropertiesUtils.fromFilesInSettingsDir("hibernate.properties", "hibernate.properties.dev");
  }
}
