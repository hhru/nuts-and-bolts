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
import javax.sql.DataSource;
import org.apache.commons.collections.BeanMap;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;

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
    bind(SessionFactory.class).annotatedWith(ann).toProvider(hibernateAccessorProvider(name, entities))
            .in(Scopes.SINGLETON);
    bind(HibernateAccess.class).annotatedWith(ann).toProvider(new Provider<HibernateAccess>() {
      private Injector inj;

      @Inject
      public void setInjector(Injector inj) {
        this.inj = inj;
      }

      @Override
      public HibernateAccess get() {
        SessionFactory hiber = inj.getInstance(Key.get(SessionFactory.class, ann));
        DataSource db = inj.getInstance(Key.get(DataSource.class, ann));
        return new HibernateAccess(hiber, db);
      }
    }).in(Scopes.SINGLETON);
  }

  private Provider<SessionFactory> hibernateAccessorProvider(final String name, final Class<?>... entities) {
    return new Provider<SessionFactory>() {
      private Settings settings;

      @Inject
      public void setSettings(Settings settings) {
        this.settings = settings;
      }

      @Override
      public SessionFactory get() {
        AnnotationConfiguration cfg = new AnnotationConfiguration();
        cfg.setProperties(settings.subTree(name + ".hibernate", "hibernate"));

        for (Class<?> entity : entities)
          cfg.addAnnotatedClass(entity);
        return cfg.buildSessionFactory();
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
