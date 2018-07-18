package ru.hh.nab.datasource;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.hh.metrics.StatsDSender;
import ru.hh.nab.testbase.NabTestConfig;
import ru.hh.nab.testbase.postgres.embedded.EmbeddedPostgresDataSourceFactory;

import javax.sql.DataSource;

@Configuration
@Import({
  NabTestConfig.class,
  DataSourceProdConfig.class
})
public class DataSourceTestConfig {
  @Bean
  JdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

  @Bean
  DataSource dataSource() throws Exception {
    return EmbeddedPostgresDataSourceFactory.create();
  }

  @Bean
  StatsDSender statsDSender() {
    return Mockito.mock(StatsDSender.class);
  }
}
