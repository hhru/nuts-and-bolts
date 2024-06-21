package ru.hh.nab.hibernate;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import ru.hh.nab.hibernate.adapter.NabHibernateJpaVendorAdapter;
import ru.hh.nab.hibernate.adapter.NabHibernatePersistenceProvider;
import ru.hh.nab.hibernate.events.EventListenerRegistryPropagator;
import ru.hh.nab.hibernate.service.NabServiceContributor;
import ru.hh.nab.hibernate.service.ServiceSupplier;
import ru.hh.nab.jdbc.transaction.DataSourceContextTransactionManager;
import ru.hh.nab.jpa.NabJpaCommonConfig;

@Configuration
@Import({
    NabJpaCommonConfig.class,
    NabHibernateJpaVendorAdapter.class,
    NabHibernatePersistenceProvider.class,
    NabServiceContributor.class,
    EventListenerRegistryPropagator.class,
})
public class NabHibernateCommonConfig {

  @Primary
  @Bean
  DataSourceContextTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
    JpaTransactionManager jpaTransactionManager = new JpaTransactionManager(entityManagerFactory);
    return new DataSourceContextTransactionManager(jpaTransactionManager);
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
