package ru.hh.nab.testbase.hibernate;

import java.io.IOException;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import ru.hh.nab.datasource.DataSourceFactory;
import ru.hh.nab.hibernate.NabHibernateCommonConfig;
import ru.hh.nab.hibernate.properties.HibernatePropertiesProvider;
import ru.hh.nab.testbase.postgres.embedded.EmbeddedPostgresDataSourceFactory;

@Configuration
@Import(NabHibernateCommonConfig.class)
public class NabHibernateTestBaseConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(NabHibernateTestBaseConfig.class);

  @Bean
  DataSourceFactory dataSourceFactory() {
    return new EmbeddedPostgresDataSourceFactory();
  }

  @Bean
  HibernatePropertiesProvider hibernatePropertiesProvider() {
    Properties hibernateProperties;
    try {
      hibernateProperties = PropertiesLoaderUtils.loadProperties(new ClassPathResource("hibernate-test.properties"));
    } catch (IOException e) {
      LOGGER.debug("Properties resource not found: {}", e.getMessage());
      hibernateProperties = new Properties();
    }
    return new HibernatePropertiesProvider(hibernateProperties);
  }
}
