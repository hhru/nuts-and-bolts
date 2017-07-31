package ru.hh.nab.hibernate;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Scopes;
import com.google.inject.matcher.Matchers;
import java.lang.annotation.Annotation;
import java.util.Properties;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.sql.DataSource;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.event.PreLoadEventListener;
import org.hibernate.event.def.DefaultPreLoadEventListener;

public class HibernateModule extends AbstractModule {

  private final String dataSourceName;
  private final Class<? extends Annotation> annotation;
  private final Class<?>[] entities;

  public HibernateModule(Class<?>... entities) {
    this("default-db", Default.class, entities);
  }

  public HibernateModule(String dataSourceName, Class<? extends Annotation> annotation, Class<?>... entities) {
    this.dataSourceName = dataSourceName;
    this.annotation = annotation;
    this.entities = entities;
  }

  @Override
  protected void configure() {
    bind(DataSource.class).annotatedWith(annotation).toProvider(dataSourceProvider()).in(Scopes.SINGLETON);

    bind(EntityManagerFactory.class).annotatedWith(annotation)
            .toProvider(hibernateAccessorProvider())
            .in(Scopes.SINGLETON);

    final Provider<EntityManagerFactory> emfProvider = getProvider(Key.get(EntityManagerFactory.class, annotation));
    final TxInterceptor tx = new TxInterceptor(emfProvider);

    bind(TxInterceptor.class).annotatedWith(annotation).toInstance(tx);
    bindInterceptor(Matchers.any(), new TransactionalMatcher(annotation), tx);

    bind(EntityManager.class).annotatedWith(annotation).toProvider(tx::currentEntityManager);

    bind(CriteriaBuilder.class).annotatedWith(annotation)
            .toProvider(() -> emfProvider.get().getCriteriaBuilder())
            .in(Scopes.SINGLETON);

    bind(PostCommitHooks.class).annotatedWith(annotation).toProvider(tx::currentPostCommitHooks);
  }

  private Provider<DataSource> dataSourceProvider() {
    return new MonitoringDataSourceProvider(dataSourceName);
  }

  public String getDataSourceName() {
    return dataSourceName;
  }

  public Class<?>[] getEntities() {
    return entities;
  }

  public Class<? extends Annotation> getAnnotation() {
    return annotation;
  }

  private Provider<EntityManagerFactory> hibernateAccessorProvider() {
    return new Provider<EntityManagerFactory>() {
      private Properties hibernateProperties;
      private Injector injector;

      @Inject
      public void inject(@Named("settings.properties") Properties settingsProperties, Injector injector) {
        this.hibernateProperties = subTree(getDataSourceName() + ".hibernate", "hibernate", settingsProperties);
        this.injector = injector;
      }

      @Override
      public EntityManagerFactory get() {
        Ejb3Configuration cfg = new Ejb3Configuration();
        cfg.setProperties(hibernateProperties);

        for (Class<?> entity : getEntities()) {
          cfg.addAnnotatedClass(entity);
        }

        cfg.setListeners("pre-load", new PreLoadEventListener[]{new GuicyHibernateLoader(injector), new DefaultPreLoadEventListener()});
        cfg.setDataSource(injector.getInstance(Key.get(DataSource.class, getAnnotation())));
        configureEjb3Configuration(cfg);
        EntityManagerFactory f = cfg.buildEntityManagerFactory();
        return f;
      }
    };
  }

  /**
   * override to configure additional stuff
   *
   * @param cfg
   */
  protected void configureEjb3Configuration(Ejb3Configuration cfg) {

  }

  public static Properties subTree(String prefix, Properties properties) {
    return subTree(prefix, null, properties);
  }

  public static Properties subTree(String prefix, String newPrefix, Properties properties) {
    prefix += ".";
    if (newPrefix != null) {
      newPrefix += ".";
    } else {
      newPrefix = "";
    }
    final int prefixLength = prefix.length();
    final Properties ret = new Properties();
    for (String property : properties.stringPropertyNames()) {
      if (property.startsWith(prefix)) {
        ret.put(newPrefix + property.substring(prefixLength), properties.get(property));
      }
    }
    return ret;
  }
}
