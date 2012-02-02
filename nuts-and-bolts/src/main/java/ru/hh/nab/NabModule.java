package ru.hh.nab;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.sql.DataSource;
import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.dbcp.BasicDataSource;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.event.PreLoadEventListener;
import org.hibernate.event.def.DefaultPreLoadEventListener;
import ru.hh.nab.grizzly.RequestHandler;
import ru.hh.nab.health.limits.LeakDetector;
import ru.hh.nab.health.limits.Limit;
import ru.hh.nab.health.limits.Limits;
import ru.hh.nab.health.monitoring.Dumpable;
import ru.hh.nab.health.monitoring.StatsDumper;
import ru.hh.nab.hibernate.Default;
import ru.hh.nab.hibernate.PostCommitHooks;
import ru.hh.nab.hibernate.TransactionalMatcher;
import ru.hh.nab.hibernate.TxInterceptor;
import ru.hh.nab.scopes.RequestScope;
import ru.hh.nab.scopes.ThreadLocalScope;
import ru.hh.nab.scopes.ThreadLocalScoped;
import ru.hh.nab.security.Permissions;
import ru.hh.nab.security.SecureInterceptor;
import ru.hh.nab.security.SecureMatcher;
import ru.hh.nab.security.UnauthorizedExceptionJerseyMapper;

@SuppressWarnings("UnusedDeclaration")
public abstract class NabModule extends AbstractModule {
  private final List<ScheduledTaskDef> taskDefs = Lists.newArrayList();
  private final ServletDefs servletDefs = new ServletDefs();
  private final GrizzletDefs grizzletDefs = new GrizzletDefs();

  private String defaultFreemarkerLayout = "nab/empty";

  @Override
  protected final void configure() {
    configureApp();
    bindScheduler();
    bindServlets();
    bindGrizzlets();

    bindScope(ThreadLocalScoped.class, ThreadLocalScope.THREAD_LOCAL);

    bind(UnauthorizedExceptionJerseyMapper.class);
    bindInterceptor(Matchers.any(), new SecureMatcher(),
            new SecureInterceptor(getProvider(Permissions.class)));
    schedulePeriodicTask(StatsDumper.class, 10, TimeUnit.SECONDS);
    schedulePeriodicTask(LeakDetector.class, 10, TimeUnit.SECONDS);
  }

  protected abstract void configureApp();

  protected final void bindResource(Class<?> resource) {
    bind(resource);
  }

  protected final void setDefaultFreeMarkerLayout(String layout) {
    defaultFreemarkerLayout = layout;
  }

  @Provides
  @Named("defaultFreeMarkerLayout")
  protected final String defaultFreeMarkerLayout() {
    return defaultFreemarkerLayout;
  }

  protected final void bindDataSourceAndEntityManagerAccessor(Class<?>... entities) {
    bindDefaultDataSource();
    bindDefaultEntityManagerAccessor(entities);
  }

  protected final void bindDataSourceAndEntityManagerAccessor(String name, Class<? extends Annotation> ann,
                                                              Class<?>... entities) {
    bindDataSource(name, ann);
    bindEntityManagerAccessor(name, ann, entities);
  }

  protected final void bindDefaultEntityManagerAccessor(Class<?>... entities) {
    bindEntityManagerAccessor("default-db", Default.class, entities);
  }

  protected final void bindEntityManagerAccessor(String name, final Class<? extends Annotation> ann, Class<?>... entities) {
    bind(EntityManagerFactory.class).annotatedWith(ann).toProvider(hibernateAccessorProvider(name, ann, entities))
            .in(Scopes.SINGLETON);

    final Provider<EntityManagerFactory> emfProvider = getProvider(Key.get(EntityManagerFactory.class, ann));
    final TxInterceptor tx = new TxInterceptor(emfProvider);

    bind(TxInterceptor.class).annotatedWith(ann).toInstance(tx);
    bindInterceptor(Matchers.any(), new TransactionalMatcher(ann), tx);

    bind(EntityManager.class).annotatedWith(ann).toProvider(new Provider<EntityManager>() {
      @Override
      public EntityManager get() {
        return tx.currentEntityManager();
      }
    });

    bind(ModelAccess.class).annotatedWith(ann).toProvider(new Provider<ModelAccess>() {
      @Override
      public ModelAccess get() {
        return new ModelAccess(emfProvider);
      }
    }).in(Scopes.SINGLETON);

    bind(CriteriaBuilder.class).annotatedWith(ann).toProvider(new Provider<CriteriaBuilder>() {
      @Override
      public CriteriaBuilder get() {
        return emfProvider.get().getCriteriaBuilder();
      }
    }).in(Scopes.SINGLETON);

    bind(PostCommitHooks.class).annotatedWith(ann).toProvider(new Provider<PostCommitHooks>() {
      @Override
      public PostCommitHooks get() {
        return tx.currentPostCommitHooks();
      }
    });
  }

  protected final void bindServlet(String pattern, Class<? extends HttpServlet> klass) {
    servletDefs.add(new ServletDef(klass, pattern));
  }

  protected final void bindGrizzlet(Class<? extends RequestHandler> handler) {
    grizzletDefs.add(new GrizzletDef(handler));
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

  protected final void bindDefaultDataSource() {
    bind(DataSource.class).annotatedWith(Default.class).toProvider(dataSourceProvider("default-db")).in(Scopes.SINGLETON);
  }

  private Provider<DataSource> dataSourceProvider(final String name) {
    return new Provider<DataSource>() {
      private Settings settings;

      @Inject
      public void setSettings(Settings settings) {
        this.settings = settings;
      }

      @SuppressWarnings({"unchecked"})
      @Override
      public DataSource get() {
        Properties c3p0Props = settings.subTree(name + ".c3p0");
        Properties dbcpProps = settings.subTree(name + ".dbcp");

        Preconditions.checkState(c3p0Props.isEmpty() || dbcpProps.isEmpty(),
                "Both c3p0 and dbcp settings are present");
        if (!c3p0Props.isEmpty()) {
          ComboPooledDataSource ds = new ComboPooledDataSource();
          new BeanMap(ds).putAll(c3p0Props);
          return ds;
        }
        if (!dbcpProps.isEmpty()) {
          BasicDataSource ds = new BasicDataSource();
          new BeanMap(ds).putAll(dbcpProps);
          return ds;
        }

        throw new IllegalStateException("Neither c3p0 nor dbcp settings found");
      }
    };
  }

  private void bindScheduler() {
    bind(Key.get(ScheduledExecutorService.class, Names.named("system"))).toProvider(
        new Provider<ScheduledExecutorService>() {
          @Inject
          public Injector injector;

          @Override
          public ScheduledExecutorService get() {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            for (ScheduledTaskDef taskDef : taskDefs) {
              Runnable r = injector.getInstance(taskDef.klass);
              scheduler.scheduleAtFixedRate(r, (long) (taskDef.time * Math.random()), taskDef.time,
                  taskDef.unit);
            }
            return Executors.unconfigurableScheduledExecutorService(scheduler);
          }
        }).asEagerSingleton();
  }

  private void bindServlets() {
    bind(ServletDefs.class).toInstance(servletDefs);
  }

  private void bindGrizzlets() {
    bind(GrizzletDefs.class).toInstance(grizzletDefs);
  }

  static class ServletDefs extends ArrayList<ServletDef> {
  }

  static class GrizzletDefs extends ArrayList<GrizzletDef> {
  }

  static class ServletDef {
    final Class<? extends Servlet> servlet;
    final String pattern;

    private ServletDef(Class<? extends Servlet> servlet, String pattern) {
      this.servlet = servlet;
      this.pattern = pattern;
    }
  }

  private static class ScheduledTaskDef {
    private final Class<? extends Runnable> klass;
    private final long time;
    private final TimeUnit unit;

    private ScheduledTaskDef(Class<? extends Runnable> klass, long time, TimeUnit unit) {
      this.klass = klass;
      this.time = time;
      this.unit = unit;
    }
  }

  static class GrizzletDef {
    final Class<? extends RequestHandler> handlerClass;

    public GrizzletDef(Class<? extends RequestHandler> handlerClass) {
      this.handlerClass = handlerClass;
    }
  }

  protected final void schedulePeriodicTask(Class<? extends Runnable> task, long time, TimeUnit unit) {
    taskDefs.add(new ScheduledTaskDef(task, time, unit));
  }

  protected
  @Provides
  @Singleton
  Random random(SecureRandom seed) {
    return new Random(System.nanoTime() ^ (System.currentTimeMillis() << 32) ^ seed.nextLong());
  }

  protected
  @Provides
  @Singleton
  SecureRandom secureRandom() {
    return new SecureRandom();
  }

  @Provides
  @Singleton
  protected LeakDetector detector(Provider<RequestScope.RequestScopeClosure> requestProvider) {
    return new LeakDetector(1000 * 60 * 5, requestProvider);
  }

  @Provides
  @Singleton
  protected Limits limits(@Named("limits-with-names") List<SettingsModule.LimitWithNameAndHisto> limits) throws IOException {
    Map<String, Limit> ls = Maps.newHashMap();
    for (SettingsModule.LimitWithNameAndHisto l : limits) {
      ls.put(l.name, l.limit);
    }
    return new Limits(ls);
  }

  @Provides
  @Singleton
  protected StatsDumper statsDumper(@Named("limits-with-names") List<SettingsModule.LimitWithNameAndHisto> limits) {
    Map<String, Dumpable> ls = Maps.newHashMap();
    for (SettingsModule.LimitWithNameAndHisto l : limits) {
      if (l.histo != null)
        ls.put(l.name, l.histo);
    }
    return new StatsDumper(ls);
  }
}
