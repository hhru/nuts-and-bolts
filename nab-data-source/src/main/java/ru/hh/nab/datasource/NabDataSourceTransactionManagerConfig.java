package ru.hh.nab.datasource;

import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.support.JdbcTransactionManager;
import ru.hh.nab.datasource.transaction.DataSourceContextTransactionManager;

@Configuration
public class NabDataSourceTransactionManagerConfig {
  @Bean
  DataSourceContextTransactionManager defaultJdbcTransactionManager(DataSource dataSource) {
    JdbcTransactionManager jdbcTransactionManager = new JdbcTransactionManager(dataSource);
    jdbcTransactionManager.setLazyInit(false);
    jdbcTransactionManager.afterPropertiesSet();
    return new DataSourceContextTransactionManager(jdbcTransactionManager);
  }
}
