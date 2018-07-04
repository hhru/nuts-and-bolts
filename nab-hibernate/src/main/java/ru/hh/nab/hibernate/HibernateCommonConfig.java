package ru.hh.nab.hibernate;

import org.hibernate.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import ru.hh.nab.hibernate.transaction.ExecuteOnDataSourceAspect;
import ru.hh.nab.hibernate.transaction.DataSourceContextTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@EnableAspectJAutoProxy
public class HibernateCommonConfig {

  @Bean
  LocalSessionFactoryBean sessionFactory(DataSource dataSource, Properties hibernateProperties, MappingConfig mappingConfig) {
    LocalSessionFactoryBean sessionFactoryBean = new NabSessionFactoryBean(dataSource, hibernateProperties);
    sessionFactoryBean.setDataSource(dataSource);
    sessionFactoryBean.setAnnotatedClasses(mappingConfig.getAnnotatedClasses());
    sessionFactoryBean.setPackagesToScan(mappingConfig.getPackagesToScan());
    sessionFactoryBean.setHibernateProperties(hibernateProperties);
    return sessionFactoryBean;
  }

  @Bean
  PlatformTransactionManager transactionManager(SessionFactory sessionFactory, DataSource dataSource) {
    HibernateTransactionManager hibernateTransactionManager = new HibernateTransactionManager(sessionFactory);
    hibernateTransactionManager.setDataSource(dataSource);
    return new DataSourceContextTransactionManager(hibernateTransactionManager);
  }

  @Bean
  ExecuteOnDataSourceAspect executeOnDataSourceAspect(PlatformTransactionManager transactionManager, SessionFactory sessionFactory) {
    return new ExecuteOnDataSourceAspect(transactionManager, sessionFactory);
  }
}
