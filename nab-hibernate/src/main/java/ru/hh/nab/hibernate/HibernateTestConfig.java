package ru.hh.nab.hibernate;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import ru.hh.nab.datasource.postgres.embedded.EmbeddedPostgresDataSourceFactory;

import javax.sql.DataSource;

@Configuration
public class HibernateTestConfig {
  @Bean
  DataSource dataSource() throws Exception {
    return EmbeddedPostgresDataSourceFactory.create();
  }

  @Bean
  PropertiesFactoryBean hibernateProperties() {
    PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
    propertiesFactoryBean.setLocation(new ClassPathResource("hibernate-test.properties"));
    propertiesFactoryBean.setIgnoreResourceNotFound(true);
    return propertiesFactoryBean;
  }
}
