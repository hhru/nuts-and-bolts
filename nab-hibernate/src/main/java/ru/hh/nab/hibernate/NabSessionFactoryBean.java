package ru.hh.nab.hibernate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.service.Service;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.orm.hibernate5.LocalSessionFactoryBuilder;
import ru.hh.nab.hibernate.interceptor.ControllerPassingInterceptor;
import ru.hh.nab.hibernate.interceptor.RequestIdPassingInterceptor;
import ru.hh.nab.hibernate.qualifier.Hibernate;

public final class NabSessionFactoryBean extends LocalSessionFactoryBean {

  private final Collection<ServiceSupplier<Service>> serviceSuppliers;
  private final Collection<SessionFactoryCreationHandler> sessionFactoryCreationHandlers;

  public NabSessionFactoryBean(DataSource dataSource, @Hibernate Properties hibernateProperties,
                               BootstrapServiceRegistryBuilder bootstrapServiceRegistryBuilder, Collection<ServiceSupplier<Service>> serviceSuppliers,
                               Collection<SessionFactoryCreationHandler> sessionFactoryCreationHandlers) {
    this.serviceSuppliers = new ArrayList<>(serviceSuppliers);
    this.sessionFactoryCreationHandlers = new ArrayList<>(sessionFactoryCreationHandlers);
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
    String interceptorClassName = hibernateProperties.getProperty(AvailableSettings.STATEMENT_INSPECTOR);
    if (interceptorClassName == null) {
      return;
    }

    if (!interceptorClassName.equals(RequestIdPassingInterceptor.class.getCanonicalName())
        && !interceptorClassName.equals(ControllerPassingInterceptor.class.getCanonicalName())) {
      String msg = String.format("unknown value of hibernate '%s' property", AvailableSettings.STATEMENT_INSPECTOR);
      throw new RuntimeException(msg);
    }
  }

  @Override
  protected SessionFactory buildSessionFactory(LocalSessionFactoryBuilder sfb) {
    StandardServiceRegistryBuilder serviceRegistryBuilder = sfb.getStandardServiceRegistryBuilder();
    serviceSuppliers.forEach(serviceSupplier -> {
      Service service = serviceSupplier.get();
      serviceRegistryBuilder.addService(serviceSupplier.getClazz(), service);
    });
    return sfb.buildSessionFactory();
  }

  @Override
  public SessionFactory getObject() {
    SessionFactory sessionFactory = super.getObject();
    sessionFactoryCreationHandlers.forEach(handler -> handler.accept(sessionFactory));
    return sessionFactory;
  }

  @FunctionalInterface
  public interface SessionFactoryCreationHandler extends Consumer<SessionFactory> {}

  public interface ServiceSupplier<T extends Service> extends Supplier<T> {
    Class<T> getClazz();
  }
}
