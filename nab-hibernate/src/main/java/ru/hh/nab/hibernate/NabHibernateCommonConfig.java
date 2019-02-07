package ru.hh.nab.hibernate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.integrator.spi.Integrator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.orm.hibernate5.HibernateTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import ru.hh.nab.hibernate.transaction.DataSourceContextTransactionManager;
import ru.hh.nab.hibernate.transaction.ExecuteOnDataSourceAspect;

@Configuration
@EnableTransactionManagement
@EnableAspectJAutoProxy
public class NabHibernateCommonConfig {

  @Bean
  DataSourceContextTransactionManager transactionManager(SessionFactory sessionFactory, DataSource routingDataSource) {
    HibernateTransactionManager simpleTransactionManager = new HibernateTransactionManager(sessionFactory);
    simpleTransactionManager.setDataSource(routingDataSource);
    return new DataSourceContextTransactionManager(simpleTransactionManager);
  }

  @Bean
  ExecuteOnDataSourceAspect executeOnDataSourceAspect(DataSourceContextTransactionManager transactionManager, SessionFactory sessionFactory) {
    return new ExecuteOnDataSourceAspect(transactionManager, sessionFactory);
  }

  @Bean
  NabSessionFactoryBean sessionFactoryBean(DataSource routingDataSource, Properties hibernateProperties,
    BootstrapServiceRegistryBuilder bootstrapServiceRegistryBuilder, MappingConfig mappingConfig,
    Optional<Collection<NabSessionFactoryBean.ServiceSupplier<?>>> serviceSuppliers,
    Optional<Collection<NabSessionFactoryBean.SessionFactoryCreationHandler>> sessionFactoryCreationHandlers) {
    NabSessionFactoryBean sessionFactoryBean = new NabSessionFactoryBean(routingDataSource, hibernateProperties, bootstrapServiceRegistryBuilder,
      serviceSuppliers.orElseGet(ArrayList::new), sessionFactoryCreationHandlers.orElseGet(ArrayList::new));
    sessionFactoryBean.setDataSource(routingDataSource);
    sessionFactoryBean.setAnnotatedClasses(mappingConfig.getAnnotatedClasses());
    sessionFactoryBean.setPackagesToScan(mappingConfig.getPackagesToScan());
    sessionFactoryBean.setHibernateProperties(hibernateProperties);
    return sessionFactoryBean;
  }

  @Bean
  BootstrapServiceRegistryBuilder bootstrapServiceRegistryBuilder(Optional<Collection<Integrator>> integratorsOptional) {
    BootstrapServiceRegistryBuilder bootstrapServiceRegistryBuilder = new BootstrapServiceRegistryBuilder();
    integratorsOptional.ifPresent(integrators -> integrators.forEach(bootstrapServiceRegistryBuilder::applyIntegrator));
    return bootstrapServiceRegistryBuilder;
  }

  @Bean
  NabSessionFactoryBean.ServiceSupplier<?> nabSessionFactoryBuilderServiceSupplier() {
    return new NabSessionFactoryBean.ServiceSupplier<NabSessionFactoryBuilderFactory.BuilderService>() {
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
