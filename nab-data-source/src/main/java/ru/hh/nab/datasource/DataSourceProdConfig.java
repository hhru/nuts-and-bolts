package ru.hh.nab.datasource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hh.metrics.StatsDSender;
import ru.hh.nab.datasource.monitoring.MonitoringDataSourceFactory;

@Configuration
public class DataSourceProdConfig {

  @Bean
  DataSourceFactory dataSourceFactory(String serviceName, StatsDSender statsDSender) {
    MonitoringDataSourceFactory monitoringDataSourceFactory = new MonitoringDataSourceFactory(serviceName, statsDSender);
    return new DataSourceFactory(monitoringDataSourceFactory);
  }
}
