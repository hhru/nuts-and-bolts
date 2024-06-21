package ru.hh.nab.hibernate.adapter;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceUnitInfo;
import java.util.List;
import java.util.Map;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.hibernate.service.spi.ServiceContributor;

public class NabHibernatePersistenceProvider extends HibernatePersistenceProvider {

  private final List<ServiceContributor> serviceContributors;

  public NabHibernatePersistenceProvider(List<ServiceContributor> serviceContributors) {
    this.serviceContributors = serviceContributors;
  }

  @Override
  public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
    return new EntityManagerFactoryBuilderImpl(new PersistenceUnitInfoDescriptor(info), properties) {
      @Override
      protected StandardServiceRegistryBuilder getStandardServiceRegistryBuilder(BootstrapServiceRegistry bsr) {
        StandardServiceRegistryBuilder ssrBuilder = super.getStandardServiceRegistryBuilder(bsr);
        serviceContributors.forEach(serviceContributor -> serviceContributor.contribute(ssrBuilder));
        return ssrBuilder;
      }
    }.build();
  }
}
