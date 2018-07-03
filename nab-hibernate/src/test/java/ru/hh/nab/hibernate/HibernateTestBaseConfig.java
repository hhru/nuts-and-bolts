package ru.hh.nab.hibernate;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.hibernate.model.TestEntity;

@Configuration
@Import({
  HibernateTestConfig.class,
  HibernateCommonConfig.class
})
public class HibernateTestBaseConfig {
  static final String TEST_PACKAGE = "ru.hh.nab.hibernate.model.test";

  @Bean
  MappingConfig mappingConfig() {
    MappingConfig mappingConfig = new MappingConfig(TestEntity.class);
    mappingConfig.addPackagesToScan(TEST_PACKAGE);
    return mappingConfig;
  }
}
