package ru.hh.nab.hibernate.service;

import java.util.List;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.Service;
import org.hibernate.service.spi.ServiceContributor;

public class NabServiceContributor implements ServiceContributor {

  private final List<ServiceSupplier<?>> serviceSuppliers;

  public NabServiceContributor(List<ServiceSupplier<?>> serviceSuppliers) {
    this.serviceSuppliers = serviceSuppliers;
  }

  @Override
  public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
    serviceSuppliers.forEach(serviceSupplier -> apply(serviceRegistryBuilder, serviceSupplier));
  }

  private static <T extends Service> void apply(StandardServiceRegistryBuilder serviceRegistryBuilder, ServiceSupplier<T> serviceSupplier) {
    serviceRegistryBuilder.addService(serviceSupplier.getClazz(), serviceSupplier.get());
  }
}
