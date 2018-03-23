package ru.hh.nab.datasource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.hh.nab.common.util.FileSettings;
import ru.hh.nab.core.CoreCommonConfig;
import ru.hh.nab.datasource.postgres.embedded.EmbeddedPostgresDataSourceFactory;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@Import({
  CoreCommonConfig.class,
  DataSourceProdConfig.class
})
public class DataSourceTestConfig {
  @Bean
  FileSettings fileSettings() {
    Properties properties = new Properties();
    properties.setProperty("serviceName", "DataSourceTest");
    return new FileSettings(properties);
  }

  @Bean
  JdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

  @Bean
  DataSource dataSource() throws Exception {
    return EmbeddedPostgresDataSourceFactory.create();
  }
}
