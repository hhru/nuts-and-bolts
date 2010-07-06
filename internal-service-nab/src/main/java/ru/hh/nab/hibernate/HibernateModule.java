package ru.hh.nab.hibernate;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateModule extends AbstractModule {

  @Override
  protected void configure() {
    bindInterceptor(
        Matchers.any(),
        Matchers.annotatedWith(Transactional.class),
        new TxInterceptor(getProvider(Session.class)));
  }

  @Provides
  @Singleton
  protected SessionFactory sessionFactory(Configuration config) {
    return config.buildSessionFactory();
  }

  @Provides
  protected Session session(SessionFactory sessionFactory) {
    return sessionFactory.getCurrentSession();
  }
}
