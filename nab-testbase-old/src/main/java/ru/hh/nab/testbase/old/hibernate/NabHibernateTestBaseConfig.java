package ru.hh.nab.testbase.old.hibernate;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import ru.hh.nab.datasource.DataSourceFactory;
import ru.hh.nab.hibernate.qualifier.Hibernate;
import ru.hh.nab.testbase.old.postgres.embedded.EmbeddedPostgresDataSourceFactory;

@Configuration
@Deprecated
public class NabHibernateTestBaseConfig {
  @Bean
  DataSourceFactory dataSourceFactory() {
    return new EmbeddedPostgresDataSourceFactory();
  }

  @Bean
  @Hibernate
  PropertiesFactoryBean hibernateProperties() {
    PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
    propertiesFactoryBean.setLocation(new ClassPathResource("hibernate-test.properties"));
    propertiesFactoryBean.setIgnoreResourceNotFound(true);
    return propertiesFactoryBean;
  }
}
