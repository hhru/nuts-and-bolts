package ru.hh.nab.datasource;

import java.util.function.Function;
import static java.util.stream.Collectors.toMap;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import ru.hh.nab.datasource.aspect.ExecuteOnDataSourceAspect;
import ru.hh.nab.datasource.routing.RoutingDataSourceFactory;
import ru.hh.nab.datasource.transaction.DataSourceContextTransactionManager;
import ru.hh.nab.datasource.transaction.TransactionalScope;
import ru.hh.nab.datasource.validation.DataSourcesReadyTarget;
import ru.hh.nab.datasource.validation.ExecuteOnDataSourceBeanPostProcessor;

@Configuration
@EnableTransactionManagement(order = 0)
@EnableAspectJAutoProxy
@Import({
    RoutingDataSourceFactory.class,
    ExecuteOnDataSourceBeanPostProcessor.class,
    DataSourcesReadyTarget.class,
    TransactionalScope.class,
})
public class NabDataSourceCommonConfig {

  @Bean
  ExecuteOnDataSourceAspect executeOnDataSourceAspect(ApplicationContext applicationContext) {
    var txManagers = Stream
        .of(applicationContext.getBeanNamesForType(DataSourceContextTransactionManager.class))
        .collect(toMap(Function.identity(), beanName -> applicationContext.getBean(beanName, DataSourceContextTransactionManager.class)));
    return new ExecuteOnDataSourceAspect(applicationContext.getBean(DataSourceContextTransactionManager.class), txManagers);
  }

  @Bean
  DataSourceContextTransactionManager defaultJdbcTransactionManager(DataSource dataSource) {
    JdbcTransactionManager jdbcTransactionManager = new JdbcTransactionManager(dataSource);
    jdbcTransactionManager.setLazyInit(false);
    jdbcTransactionManager.afterPropertiesSet();
    return new DataSourceContextTransactionManager(jdbcTransactionManager);
  }
}
