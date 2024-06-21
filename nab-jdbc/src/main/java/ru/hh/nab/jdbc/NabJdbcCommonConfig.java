package ru.hh.nab.jdbc;

import jakarta.annotation.Nullable;
import java.util.function.Function;
import static java.util.stream.Collectors.toMap;
import java.util.stream.Stream;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import ru.hh.nab.jdbc.aspect.ExecuteOnDataSourceAspect;
import ru.hh.nab.jdbc.aspect.ExecuteOnDataSourceTransactionCallbackFactory;
import ru.hh.nab.jdbc.routing.RoutingDataSourceFactory;
import ru.hh.nab.jdbc.transaction.TransactionalScope;
import ru.hh.nab.jdbc.validation.DataSourcesReadyTarget;
import ru.hh.nab.jdbc.validation.ExecuteOnDataSourceBeanPostProcessor;

@Configuration
@Import({
    RoutingDataSourceFactory.class,
    TransactionalScope.class,
    DataSourcesReadyTarget.class,
    ExecuteOnDataSourceBeanPostProcessor.class,
})
public class NabJdbcCommonConfig {

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
