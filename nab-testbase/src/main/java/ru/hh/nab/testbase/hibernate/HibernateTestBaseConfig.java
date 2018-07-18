package ru.hh.nab.testbase.hibernate;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import ru.hh.nab.testbase.postgres.embedded.EmbeddedPostgresDataSourceFactory;

import javax.sql.DataSource;

@Configuration
public class HibernateTestBaseConfig {
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
