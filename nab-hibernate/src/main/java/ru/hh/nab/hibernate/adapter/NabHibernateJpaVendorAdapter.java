package ru.hh.nab.hibernate.adapter;

import jakarta.persistence.spi.PersistenceProvider;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

public class NabHibernateJpaVendorAdapter extends HibernateJpaVendorAdapter {

  private final HibernatePersistenceProvider hibernatePersistenceProvider;

  public NabHibernateJpaVendorAdapter(HibernatePersistenceProvider hibernatePersistenceProvider) {
    this.hibernatePersistenceProvider = hibernatePersistenceProvider;
  }

  @Override
  public PersistenceProvider getPersistenceProvider() {
    return hibernatePersistenceProvider;
  }
}
