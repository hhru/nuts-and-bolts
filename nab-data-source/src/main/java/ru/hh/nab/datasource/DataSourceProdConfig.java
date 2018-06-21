package ru.hh.nab.datasource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.metrics.StatsDSender;

@Configuration
public class DataSourceProdConfig {

  @Bean
  DataSourceFactory dataSourceFactory(String serviceName, StatsDSender statsDSender) {
    return new DataSourceFactory(serviceName, statsDSender);
  }
}
