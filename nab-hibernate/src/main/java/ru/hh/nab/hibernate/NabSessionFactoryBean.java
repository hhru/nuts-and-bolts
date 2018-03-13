package ru.hh.nab.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.orm.hibernate5.LocalSessionFactoryBuilder;
import ru.hh.nab.hibernate.interceptor.ControllerPassingInterceptor;
import ru.hh.nab.hibernate.interceptor.RequestIdPassingInterceptor;

import javax.sql.DataSource;
import java.util.Properties;

public class NabSessionFactoryBean extends LocalSessionFactoryBean {

  public NabSessionFactoryBean(DataSource dataSource, Properties hibernateProperties) {
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

    configureAddToQuery(hibernateProperties);
  }

  private void configureAddToQuery(Properties hibernateProperties) {
    String addToQueryValue = hibernateProperties.getProperty("hibernate.add_to_query");
    if (addToQueryValue == null) {
      return;
    }

    switch (addToQueryValue) {
      case "request_id":
        setEntityInterceptor(new RequestIdPassingInterceptor());
        break;
      // Request_id in sql query prevents reuse of prepared statements, because every sql query is different.
      // Controller does not prevent reuse of prepared statements, because same sql queries from the same controller can be reused.
      case "controller":
        setEntityInterceptor(new ControllerPassingInterceptor());
        break;
      default:
        throw new RuntimeException("unknown value of hibernate 'addToQuery' property");
    }
  }

  @Override
  protected SessionFactory buildSessionFactory(LocalSessionFactoryBuilder sfb) {
    StandardServiceRegistryBuilder serviceRegistryBuilder = sfb.getStandardServiceRegistryBuilder();
    serviceRegistryBuilder.addService(NabSessionFactoryBuilderFactory.BuilderService.class, new NabSessionFactoryBuilderFactory.BuilderService());
    return sfb.buildSessionFactory();
  }
}
