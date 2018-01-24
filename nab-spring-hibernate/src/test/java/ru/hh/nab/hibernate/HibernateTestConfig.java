package ru.hh.nab.hibernate;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import ru.hh.nab.hibernate.datasource.Default;
import ru.hh.nab.util.FileSettings;

import java.util.Properties;

@Configuration
public class HibernateTestConfig {

  @Bean
  Properties hibernateProperties() {
    Properties properties = new Properties();
    properties.setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
    properties.setProperty("hibernate.hbm2ddl.auto", "create");
    properties.setProperty("hibernate.show_sql", "false");
    properties.setProperty("hibernate.format_sql", "false");
    return properties;
  }

  @Bean
  FileSettings fileSettings() {
    Properties properties = new Properties();
    properties.setProperty("serviceName", "test");
    return new FileSettings(properties);
  }

  @Default
  @Bean(destroyMethod = "shutdown")
  static EmbeddedDatabase dataSource() {
    return createEmbeddedDatabase();
  }

  private static EmbeddedDatabase createEmbeddedDatabase() {
    return new EmbeddedDatabaseBuilder()
        .setType(EmbeddedDatabaseType.HSQL)
        .build();
  }
}
