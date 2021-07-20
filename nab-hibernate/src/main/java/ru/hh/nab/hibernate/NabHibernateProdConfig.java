package ru.hh.nab.hibernate;

import java.util.Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.common.properties.PropertiesUtils;
import ru.hh.nab.datasource.NabDataSourceProdConfig;

@Configuration
@Import({NabHibernateCommonConfig.class, NabDataSourceProdConfig.class})
public class NabHibernateProdConfig {

  @Bean
  Properties hibernateProperties() throws Exception {
    return PropertiesUtils.fromFilesInSettingsDir("hibernate.properties", "hibernate.properties.dev");
  }
}
