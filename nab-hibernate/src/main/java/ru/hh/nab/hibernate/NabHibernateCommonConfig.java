package ru.hh.nab.hibernate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import static java.util.stream.Collectors.toMap;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.integrator.spi.Integrator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import ru.hh.nab.hibernate.datasource.RoutingDataSourceFactory;
import ru.hh.nab.hibernate.qualifier.Hibernate;
import ru.hh.nab.hibernate.transaction.DataSourceContextTransactionManager;
import ru.hh.nab.hibernate.transaction.DataSourcesReadyTarget;
import ru.hh.nab.hibernate.transaction.ExecuteOnDataSourceAspect;
import ru.hh.nab.hibernate.transaction.ExecuteOnDataSourceBeanPostProcessor;
import ru.hh.nab.hibernate.transaction.TransactionalScope;


@Configuration
@Import({
    RoutingDataSourceFactory.class,
})
@EnableTransactionManagement(order = 0)
@EnableAspectJAutoProxy
public class NabHibernateCommonConfig {

  @Primary
  @Bean
  DataSourceContextTransactionManager transactionManager(SessionFactory sessionFactory) {
    HibernateTransactionManager simpleTransactionManager = new HibernateTransactionManager(sessionFactory);
    simpleTransactionManager.setAutodetectDataSource(true);
    return new DataSourceContextTransactionManager(simpleTransactionManager);
  }

  @Bean
  ExecuteOnDataSourceBeanPostProcessor executeOnDataSourceBeanPostProcessor() {
    return new ExecuteOnDataSourceBeanPostProcessor();
  }

  @Bean
  DataSourcesReadyTarget dataSourcesReadyTarget(List<DataSource> dataSources) {
    return new DataSourcesReadyTarget(dataSources);
  }

  @Bean
  ExecuteOnDataSourceAspect executeOnDataSourceAspect(ApplicationContext applicationContext) {
    var txManagers = Stream
        .of(applicationContext.getBeanNamesForType(DataSourceContextTransactionManager.class))
        .collect(toMap(Function.identity(), beanName -> applicationContext.getBean(beanName, DataSourceContextTransactionManager.class)));
    return new ExecuteOnDataSourceAspect(applicationContext.getBean(DataSourceContextTransactionManager.class), txManagers);
  }

  @Bean
  NabSessionFactoryBean sessionFactoryBean(
      DataSource dataSource,
      @Hibernate Properties hibernateProperties,
      BootstrapServiceRegistryBuilder bootstrapServiceRegistryBuilder,
      List<MappingConfig> mappingConfigs,
      @Nullable Collection<NabSessionFactoryBean.ServiceSupplier<?>> serviceSuppliers,
      @Nullable Collection<NabSessionFactoryBean.SessionFactoryCreationHandler> sessionFactoryCreationHandlers
  ) {
    NabSessionFactoryBean sessionFactoryBean = new NabSessionFactoryBean(
        dataSource,
        hibernateProperties,
        bootstrapServiceRegistryBuilder,
        Objects.requireNonNullElseGet(serviceSuppliers, ArrayList::new),
        Objects.requireNonNullElseGet(sessionFactoryCreationHandlers, ArrayList::new)
    );
    sessionFactoryBean.setDataSource(dataSource);

    Class<?>[] annotatedClasses = mappingConfigs.stream().flatMap(mc -> Stream.of(mc.getAnnotatedClasses())).toArray(Class[]::new);
    String[] packagesToScan = mappingConfigs.stream().flatMap(mc -> Stream.of(mc.getPackagesToScan())).toArray(String[]::new);

    sessionFactoryBean.setAnnotatedClasses(annotatedClasses);
    sessionFactoryBean.setPackagesToScan(packagesToScan);
    sessionFactoryBean.setHibernateProperties(hibernateProperties);
    return sessionFactoryBean;
  }

  @Bean
  BootstrapServiceRegistryBuilder bootstrapServiceRegistryBuilder(@Nullable Collection<Integrator> integrators) {
    BootstrapServiceRegistryBuilder bootstrapServiceRegistryBuilder = new BootstrapServiceRegistryBuilder();
    if (integrators != null) {
      integrators.forEach(bootstrapServiceRegistryBuilder::applyIntegrator);
    }
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

  @Bean
  TransactionalScope transactionalScope() {
    return new TransactionalScope();
  }
}
