package ru.hh.nab.hibernate;

import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.internal.SessionFactoryBuilderImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.AbstractDelegatingSessionFactoryOptions;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryBuilderFactory;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.service.Service;
import org.hibernate.service.UnknownServiceException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class NabSessionFactoryBuilderFactory implements SessionFactoryBuilderFactory {

  @Override
  public SessionFactoryBuilder getSessionFactoryBuilder(MetadataImplementor metadata, SessionFactoryBuilderImplementor defaultBuilder) {
    // Hibernate allows custom NabSessionFactoryBuilderFactory via java META-INF/services.
    // But because of java META-INF/services all SessionFactories will be built with this NabSessionFactoryBuilderFactory, even clobSessionFactory.
    // We do not want clob to use NabSessionFactoryBuilderFactory because of PhysicalConnectionHandlingMode.
    // There are cases that use readonly datasource and then in the readonly "transaction" switch to the clob datasource.
    // In these cases clobSessionFactory will use connection in mode DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT,
    // which will result in exception when clobSessionFactory tries to commit its transaction.
    // There are several options to avoid this:
    // - get rid of clob, transfer clob data to hhru main database;
    // - hack clob transactions not to commit in readonly mode;
    // - register BuilderService during construction of session factory, if it is not registered - fallback to defaultBuilder;
    // For now, we use the last option.
    StandardServiceRegistry serviceRegistry = metadata.getMetadataBuildingOptions().getServiceRegistry();
    BuilderService builderService;
    try {
      builderService = serviceRegistry.getService(BuilderService.class);
    } catch (UnknownServiceException e) {
      return defaultBuilder;
    }
    return builderService.createSessionFactoryBuilder(metadata);
  }

  public static class BuilderService implements Service {
    protected SessionFactoryBuilder createSessionFactoryBuilder(MetadataImplementor metadata) {
      return new HhSessionFactoryBuilder(metadata);
    }
  }

  public static class HhSessionFactoryBuilder extends SessionFactoryBuilderImpl {
    protected HhSessionFactoryBuilder(MetadataImplementor metadata) {
      super(metadata);
    }

    @Override
    public SessionFactoryOptions buildSessionFactoryOptions() {
      SessionFactoryOptions sessionFactoryOptions = super.buildSessionFactoryOptions();
      return new NabSessionFactoryOptions(sessionFactoryOptions);
    }
  }

  static class NabSessionFactoryOptions extends AbstractDelegatingSessionFactoryOptions {
    NabSessionFactoryOptions(SessionFactoryOptions delegate) {
      super(delegate);
    }

    @Override
    public PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode() {
      return TransactionSynchronizationManager.isCurrentTransactionReadOnly()
          ? PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT
          : PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION;
    }
  }
}
