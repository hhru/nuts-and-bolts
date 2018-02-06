package ru.hh.nab.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.orm.hibernate5.LocalSessionFactoryBuilder;

import javax.sql.DataSource;
import java.util.Properties;

public class NabSessionFactoryBean extends LocalSessionFactoryBean {

  NabSessionFactoryBean(DataSource dataSource, Properties hibernateProperties) {
    BootstrapServiceRegistryBuilder bootstrapServiceRegistryBuilder = new BootstrapServiceRegistryBuilder();
    MetadataSources metadataSources = new MetadataSources(bootstrapServiceRegistryBuilder.build());
    setMetadataSources(metadataSources);

    setDataSource(dataSource);

    // if set to true, it slows down acquiring database connection on application start
    hibernateProperties.setProperty("hibernate.temp.use_jdbc_metadata_defaults", "false");

    // used to retrieve natively generated keys after insert
    // if set to false, Hibernate will retrieve key directly from sequence
    // and can fail if GenerationType = IDENTITY and sequence name is non-standard
    hibernateProperties.setProperty("hibernate.jdbc.use_get_generated_keys", "true");

    setHibernateProperties(hibernateProperties);
  }

  @Override
  protected SessionFactory buildSessionFactory(LocalSessionFactoryBuilder sfb) {
    StandardServiceRegistryBuilder serviceRegistryBuilder = sfb.getStandardServiceRegistryBuilder();
    serviceRegistryBuilder.addService(NabSessionFactoryBuilderFactory.BuilderService.class, new NabSessionFactoryBuilderFactory.BuilderService());
    return sfb.buildSessionFactory();
  }
}
