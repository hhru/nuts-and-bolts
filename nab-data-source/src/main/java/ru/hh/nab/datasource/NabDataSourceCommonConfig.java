package ru.hh.nab.datasource;

import java.util.function.Function;
import static java.util.stream.Collectors.toMap;
import java.util.stream.Stream;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import ru.hh.nab.common.properties.FileSettings;
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
  ExecuteOnDataSourceAspect executeOnDataSourceAspect(FileSettings fileSettings, ApplicationContext applicationContext) {
    if (fileSettings.getBoolean("nab.datasource.executeOnDataSource.skip", false)) {
      return ExecuteOnDataSourceAspect.skipped();
    }

    var txManagers = Stream
        .of(applicationContext.getBeanNamesForType(DataSourceContextTransactionManager.class))
        .collect(toMap(Function.identity(), beanName -> applicationContext.getBean(beanName, DataSourceContextTransactionManager.class)));
    return new ExecuteOnDataSourceAspect(applicationContext.getBean(DataSourceContextTransactionManager.class), txManagers);
  }
}
