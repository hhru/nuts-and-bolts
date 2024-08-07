package ru.hh.nab.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import ru.hh.nab.datasource.NabDataSourceCommonConfig;
import ru.hh.nab.datasource.aspect.ExecuteOnDataSourceTransactionCallbackFactory;
import ru.hh.nab.datasource.transaction.DataSourceContextTransactionManager;
import ru.hh.nab.jpa.aspect.ExecuteOnDataSourceTransactionCallbackFactoryImpl;

@Configuration
@Import({
    NabDataSourceCommonConfig.class,
    EntityManagerFactoryRegistry.class,
})
public class NabJpaCommonConfig {

  @Bean
  @EntityManagerFactoryId("default")
  LocalContainerEntityManagerFactoryBean entityManagerFactory(
      DataSource dataSource,
      JpaVendorAdapter jpaVendorAdapter,
      JpaPropertiesProvider jpaPropertiesProvider,
      List<MappingConfig> mappingConfigs,
      Collection<EntityManagerFactoryCreationHandler> entityManagerFactoryCreationHandlers
  ) {
    List<String> managedClassNames = mappingConfigs
        .stream()
        .map(MappingConfig::getAnnotatedClasses)
        .flatMap(Stream::of)
        .map(Class::getCanonicalName)
        .toList();
    String[] packagesToScan = mappingConfigs
        .stream()
        .map(MappingConfig::getPackagesToScan)
        .flatMap(Stream::of)
        .toArray(String[]::new);

    NabEntityManagerFactoryBean entityManagerFactoryBean = new NabEntityManagerFactoryBean();
    entityManagerFactoryBean.setDataSource(dataSource);
    entityManagerFactoryBean.setJpaVendorAdapter(jpaVendorAdapter);
    entityManagerFactoryBean.setJpaProperties(jpaPropertiesProvider.get());
    entityManagerFactoryBean.setPackagesToScan(packagesToScan);
    entityManagerFactoryBean.setPersistenceUnitPostProcessors(new NabPersistenceUnitPostProcessor(managedClassNames));
    entityManagerFactoryBean.setEntityManagerFactoryCreationHandlers(entityManagerFactoryCreationHandlers);
    return entityManagerFactoryBean;
  }

  @Bean
  public EntityManager entityManager(EntityManagerFactory entityManagerFactory) {
    return SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory);
  }

  @Bean
  public ExecuteOnDataSourceTransactionCallbackFactory transactionCallbackFactory(EntityManager entityManager) {
    return new ExecuteOnDataSourceTransactionCallbackFactoryImpl(entityManager);
  }

  @Primary
  @Bean
  DataSourceContextTransactionManager transactionManager(
      EntityManagerFactory entityManagerFactory,
      ExecuteOnDataSourceTransactionCallbackFactory transactionCallbackFactory
  ) {
    JpaTransactionManager jpaTransactionManager = new JpaTransactionManager(entityManagerFactory);
    return new DataSourceContextTransactionManager(jpaTransactionManager, transactionCallbackFactory);
  }
}
