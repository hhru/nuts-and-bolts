package ru.hh.nab.hibernate;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
  HibernateTestConfig.class,
  HibernateCommonConfig.class
})
public class HibernateTestBaseConfig {
  @Bean
  MappingConfig mappingConfig() {
    MappingConfig mappingConfig = new MappingConfig();
    mappingConfig.addMapping(TestEntity.class);
    return mappingConfig;
  }
}
