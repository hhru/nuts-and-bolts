package ru.hh.nab.datasource;

import jakarta.annotation.Nullable;
import java.util.function.Function;
import static java.util.stream.Collectors.toMap;
import java.util.stream.Stream;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import ru.hh.nab.datasource.aspect.ExecuteOnDataSourceAspect;
import ru.hh.nab.datasource.aspect.ExecuteOnDataSourceTransactionCallbackFactory;
import ru.hh.nab.datasource.routing.RoutingDataSourceFactory;
import ru.hh.nab.datasource.transaction.TransactionalScope;
import ru.hh.nab.datasource.validation.DataSourcesReadyTarget;
import ru.hh.nab.datasource.validation.ExecuteOnDataSourceBeanPostProcessor;

@Configuration
@Import({
    RoutingDataSourceFactory.class,
    ExecuteOnDataSourceBeanPostProcessor.class,
    DataSourcesReadyTarget.class,
    TransactionalScope.class,
})
public class NabDataSourceCommonConfig {

  @Bean
  ExecuteOnDataSourceAspect executeOnDataSourceAspect(
      ApplicationContext applicationContext,
      @Nullable ExecuteOnDataSourceTransactionCallbackFactory transactionCallbackFactory
  ) {
    var txManagers = Stream
        .of(applicationContext.getBeanNamesForType(PlatformTransactionManager.class))
        .collect(toMap(Function.identity(), beanName -> applicationContext.getBean(beanName, PlatformTransactionManager.class)));
    return new ExecuteOnDataSourceAspect(applicationContext.getBean(PlatformTransactionManager.class), txManagers, transactionCallbackFactory);
  }
}
