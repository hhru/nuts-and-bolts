package ru.hh.nab.testbase.datasource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.hh.nab.datasource.DataSourceFactory;
import ru.hh.nab.datasource.NabDataSourceCommonConfig;
import ru.hh.nab.testbase.postgres.embedded.EmbeddedPostgresDataSourceFactory;

@Configuration
@Import({
    NabDataSourceCommonConfig.class,
})
public class NabDataSourceTestBaseConfig {
  @Bean
  DataSourceFactory dataSourceFactory() {
    return new EmbeddedPostgresDataSourceFactory();
  }
}
