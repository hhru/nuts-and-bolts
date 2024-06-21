package ru.hh.nab.hibernate.service;

import java.util.List;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.Service;
import org.hibernate.service.spi.ServiceContributor;

public class NabServiceContributor implements ServiceContributor {

  private final List<ServiceSupplier<Service>> serviceSuppliers;

  public NabServiceContributor(List<ServiceSupplier<Service>> serviceSuppliers) {
    this.serviceSuppliers = serviceSuppliers;
  }

  @Override
  public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
    serviceSuppliers.forEach(serviceSupplier -> serviceRegistryBuilder.addService(serviceSupplier.getClazz(), serviceSupplier.get()));
  }
}
