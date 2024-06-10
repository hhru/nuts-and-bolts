package ru.hh.nab.hibernate;

import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.datasource.DataSourceFactory;
import ru.hh.nab.datasource.DataSourceType;
import ru.hh.nab.hibernate.model.TestEntity;
import ru.hh.nab.jpa.MappingConfig;
import ru.hh.nab.testbase.NabTestConfig;
import ru.hh.nab.testbase.hibernate.NabHibernateTestBaseConfig;

@Configuration
@Import({
    NabHibernateCommonConfig.class,
    NabHibernateTestBaseConfig.class,
    NabTestConfig.class,
})
public class HibernateTestConfig {
  static final String TEST_PACKAGE = "ru.hh.nab.hibernate.model.test";

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
