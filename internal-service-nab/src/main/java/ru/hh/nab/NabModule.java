package ru.hh.nab;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.lang.annotation.Annotation;
import java.util.Properties;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.apache.commons.collections.BeanMap;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.event.PreLoadEventListener;
import org.hibernate.event.def.DefaultPreLoadEventListener;

public abstract class NabModule extends AbstractModule {
  @Override
  protected final void configure() {
    bind(JerseyDXMarshaller.class).in(Scopes.SINGLETON);
    configureApp();
  }

  protected abstract void configureApp();

  protected final void bindDataSourceAndHibernateAccessor(String name, Class<? extends Annotation> ann,
                                                          Class<?>... entities) {
    bindDataSource(name, ann);
    bindHibernateAccessor(name, ann, entities);
  }

  protected final void bindHibernateAccessor(String name, final Class<? extends Annotation> ann, Class<?>... entities) {
    bind(EntityManagerFactory.class).annotatedWith(ann).toProvider(hibernateAccessorProvider(name, ann, entities))
            .in(Scopes.SINGLETON);
    bind(ModelAccess.class).annotatedWith(ann).toProvider(new Provider<ModelAccess>() {
      private Injector inj;

      @Inject
      public void setInjector(Injector inj) {
        this.inj = inj;
      }

      @Override
      public ModelAccess get() {
        EntityManagerFactory hiber = inj.getInstance(Key.get(EntityManagerFactory.class, ann));
        return new ModelAccess(hiber);
      }
    }).in(Scopes.SINGLETON);
  }

  private Provider<EntityManagerFactory> hibernateAccessorProvider(final String name,
                                                                   final Class<? extends Annotation> ann,
                                                                   final Class<?>... entities) {
    return new Provider<EntityManagerFactory>() {
      private Settings settings;
      private Injector injector;

      @Inject
      public void inject(Settings settings, Injector injector) {
        this.settings = settings;
        this.injector = injector;
      }

      @Override
      public EntityManagerFactory get() {
        Ejb3Configuration cfg = new Ejb3Configuration();
        cfg.setProperties(settings.subTree(name + ".hibernate", "hibernate"));

        for (Class<?> entity : entities)
          cfg.addAnnotatedClass(entity);

        cfg.setListeners("pre-load", new PreLoadEventListener[]{new GuicyHibernateLoader(injector),
                new DefaultPreLoadEventListener()});
        cfg.setDataSource(injector.getInstance(Key.get(DataSource.class, ann)));
        return cfg.buildEntityManagerFactory();
      }
    };
  }

  protected final void bindDataSource(String name, Class<? extends Annotation> ann) {
    bind(DataSource.class).annotatedWith(ann).toProvider(dataSourceProvider(name)).in(Scopes.SINGLETON);
  }

  private Provider<DataSource> dataSourceProvider(final String name) {
    return new Provider<DataSource>() {
      private Settings settings;

      @Inject
      public void setSettings(Settings settings) {
        this.settings = settings;
      }

      @Override
      public DataSource get() {
        ComboPooledDataSource ds = new ComboPooledDataSource();
        Properties props = settings.subTree(name + ".c3p0");
        new BeanMap(ds).putAll(props);
        return ds;
      }
    };
  }
}
