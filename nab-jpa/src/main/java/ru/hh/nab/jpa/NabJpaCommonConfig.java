package ru.hh.nab.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.Collection;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement(order = 0)
@EnableAspectJAutoProxy
public class NabJpaCommonConfig {

  @Bean
  LocalContainerEntityManagerFactoryBean entityManagerFactoryBean(
      DataSource dataSource,
      JpaVendorAdapter jpaVendorAdapter,
      JpaPropertiesProvider jpaPropertiesProvider,
      Collection<EntityManagerFactoryCreationHandler> entityManagerFactoryCreationHandlers
  ) {
    NabEntityManagerFactoryBean entityManagerFactoryBean = new NabEntityManagerFactoryBean();
    entityManagerFactoryBean.setDataSource(dataSource);
    entityManagerFactoryBean.setJpaVendorAdapter(jpaVendorAdapter);
    entityManagerFactoryBean.setJpaProperties(jpaPropertiesProvider.get());
    entityManagerFactoryBean.setEntityManagerFactoryCreationHandlers(entityManagerFactoryCreationHandlers);
    return entityManagerFactoryBean;
  }

  @Bean
  public static EntityManager sharedEntityManager(EntityManagerFactory emf) {
    return SharedEntityManagerCreator.createSharedEntityManager(emf);
  }
}
