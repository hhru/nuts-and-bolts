package ru.hh.nab.hibernate;

import jakarta.persistence.EntityManagerFactory;
import java.util.function.Function;
import static java.util.stream.Collectors.toMap;
import java.util.stream.Stream;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.EntityManagerProxy;
import org.springframework.orm.jpa.JpaTransactionManager;
import ru.hh.nab.hibernate.adapter.NabHibernateJpaVendorAdapter;
import ru.hh.nab.hibernate.adapter.NabHibernatePersistenceProvider;
import ru.hh.nab.hibernate.datasource.RoutingDataSourceFactory;
import ru.hh.nab.hibernate.events.EventListenerRegistryPropagator;
import ru.hh.nab.hibernate.service.NabServiceContributor;
import ru.hh.nab.hibernate.service.ServiceSupplier;
import ru.hh.nab.hibernate.transaction.DataSourceContextTransactionManager;
import ru.hh.nab.hibernate.transaction.DataSourcesReadyTarget;
import ru.hh.nab.hibernate.transaction.ExecuteOnDataSourceAspect;
import ru.hh.nab.hibernate.transaction.ExecuteOnDataSourceBeanPostProcessor;
import ru.hh.nab.hibernate.transaction.TransactionalScope;
import ru.hh.nab.jpa.NabJpaCommonConfig;

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
})
public class NabHibernateCommonConfig {

  @Primary
  @Bean
  DataSourceContextTransactionManager transactionManager(EntityManagerFactory entityManagerFactory, EntityManagerProxy entityManagerProxy) {
    JpaTransactionManager jpaTransactionManager = new JpaTransactionManager(entityManagerFactory);
    return new DataSourceContextTransactionManager(jpaTransactionManager, entityManagerProxy);
  }

  @Bean
  ExecuteOnDataSourceAspect executeOnDataSourceAspect(ApplicationContext applicationContext) {
    var txManagers = Stream
        .of(applicationContext.getBeanNamesForType(DataSourceContextTransactionManager.class))
        .collect(toMap(Function.identity(), beanName -> applicationContext.getBean(beanName, DataSourceContextTransactionManager.class)));
    return new ExecuteOnDataSourceAspect(applicationContext.getBean(DataSourceContextTransactionManager.class), txManagers);
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
