package ru.hh.nab.jpa;

import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.datasource.DataSourceFactory;
import ru.hh.nab.datasource.DataSourceType;
import ru.hh.nab.jpa.model.TestEntity;
import ru.hh.nab.testbase.NabTestConfig;
import ru.hh.nab.testbase.hibernate.NabHibernateTestBaseConfig;

@Configuration
@Import({
    NabHibernateTestBaseConfig.class,
    NabTestConfig.class,
})
public class JpaTestConfig {
  static final String TEST_PACKAGE = "ru.hh.nab.jpa.model.test";

  @Bean
  MappingConfig mappingConfig() {
    MappingConfig mappingConfig = new MappingConfig(TestEntity.class);
    mappingConfig.addPackagesToScan(TEST_PACKAGE);
    return mappingConfig;
  }

  @Bean
  DataSource dataSource(DataSourceFactory dataSourceFactory, FileSettings fileSettings) {
    return dataSourceFactory.create(DataSourceType.MASTER, false, fileSettings);
  }
}
