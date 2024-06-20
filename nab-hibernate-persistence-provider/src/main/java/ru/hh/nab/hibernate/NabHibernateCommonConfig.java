package ru.hh.nab.hibernate;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManagerFactory;
import java.util.function.Function;
import static java.util.stream.Collectors.toMap;
import java.util.stream.Stream;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import ru.hh.nab.hibernate.adapter.NabHibernateJpaVendorAdapter;
import ru.hh.nab.hibernate.adapter.NabHibernatePersistenceProvider;
import ru.hh.nab.hibernate.events.EventListenerRegistryPropagator;
import ru.hh.nab.hibernate.service.NabServiceContributor;
import ru.hh.nab.hibernate.service.ServiceSupplier;
import ru.hh.nab.jdbc.aspect.ExecuteOnDataSourceAspect;
import ru.hh.nab.jdbc.aspect.ExecuteOnDataSourceTransactionCallbackFactory;
import ru.hh.nab.jdbc.routing.RoutingDataSourceFactory;
import ru.hh.nab.jdbc.transaction.DataSourceContextTransactionManager;
import ru.hh.nab.jdbc.transaction.TransactionalScope;
import ru.hh.nab.jdbc.validation.DataSourcesReadyTarget;
import ru.hh.nab.jdbc.validation.ExecuteOnDataSourceBeanPostProcessor;
import ru.hh.nab.jpa.NabJpaCommonConfig;
import ru.hh.nab.jpa.aspect.ExecuteOnDataSourceTransactionCallbackFactoryImpl;

@Configuration
@Import({
    RoutingDataSourceFactory.class,
    NabJpaCommonConfig.class,
    NabHibernateJpaVendorAdapter.class,
    NabHibernatePersistenceProvider.class,
    NabServiceContributor.class,
    EventListenerRegistryPropagator.class,
    ExecuteOnDataSourceBeanPostProcessor.class,
    DataSourcesReadyTarget.class,
    TransactionalScope.class,
    ExecuteOnDataSourceTransactionCallbackFactoryImpl.class,
})
public class NabHibernateCommonConfig {

  @Primary
  @Bean
  DataSourceContextTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
    JpaTransactionManager jpaTransactionManager = new JpaTransactionManager(entityManagerFactory);
    return new DataSourceContextTransactionManager(jpaTransactionManager);
  }

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

  @Bean
  ServiceSupplier<?> nabSessionFactoryBuilderServiceSupplier() {
    return new ServiceSupplier<NabSessionFactoryBuilderFactory.BuilderService>() {
      @Override
      public Class<NabSessionFactoryBuilderFactory.BuilderService> getClazz() {
        return NabSessionFactoryBuilderFactory.BuilderService.class;
      }

      @Override
      public NabSessionFactoryBuilderFactory.BuilderService get() {
        return new NabSessionFactoryBuilderFactory.BuilderService();
      }
    };
  }
}
