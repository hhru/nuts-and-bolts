package ru.hh.nab;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.sql.DataSource;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
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
import ru.hh.nab.health.monitoring.MethodProbingInterceptor;
import ru.hh.nab.health.monitoring.Probe;
import ru.hh.nab.health.monitoring.StatsDumper;
import ru.hh.nab.hibernate.Default;
import ru.hh.nab.hibernate.PostCommitHooks;
import ru.hh.nab.hibernate.Transactional;
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
    bindInterceptor(Matchers.any(), new SecureMatcher(), new SecureInterceptor(getProvider(Permissions.class)));
    schedulePeriodicTask(StatsDumper.class, 10, TimeUnit.SECONDS);
    schedulePeriodicTask(LeakDetector.class, 10, TimeUnit.SECONDS);

    bindInterceptor(Matchers.any(), Matchers.annotatedWith(Probe.class), MethodProbingInterceptor.INSTANCE);
  }

  protected abstract void configureApp();

  private static Matcher<Class> subclassesMatcher(final Class<?>... classes) {
    return new AbstractMatcher<Class>() {
      final List<Class<?>> superclasses = Lists.newArrayList(classes);

      @Override
      public boolean matches(Class subclass) {
        for (Class<?> superclass : superclasses) {
          if (superclass.isAssignableFrom(subclass)) {
            return true;
          }
        }
        return false;
      }

      @Override
      public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("subclassesOf(");
        for (Class<?> superclass : superclasses) {
          sb.append(superclass.getSimpleName());
          sb.append(".class,");
        }
        sb.setLength(sb.length() - 1);
        sb.append(')');
        return sb.toString();
      }
    };
  }

  protected final void bindJerseyResources(final Class<?>... classes) {
    for (Class<?> clazz : classes) {
      bind(clazz);
    }
    bindInterceptor(
      subclassesMatcher(classes),
      new AbstractMatcher<AnnotatedElement>() {
        @Override
        public boolean matches(AnnotatedElement annotatedElement) {
          for (Annotation a : annotatedElement.getAnnotations()) {
            if (a instanceof Path) {
              return true;
            }
            if (a.annotationType().isAnnotationPresent(HttpMethod.class)) {
              return true;
            }
          }
          return false;
        }
      }, MethodProbingInterceptor.INSTANCE);
  }

  protected final void bindGrizzletResources(Class<? extends RequestHandler>... handlers) {
    for (Class<? extends RequestHandler> handler : handlers) {
      bind(handler);
      grizzletDefs.add(new GrizzletDef(handler));
    }
    bindInterceptor(subclassesMatcher(handlers), Matchers.any(), MethodProbingInterceptor.INSTANCE);
  }

  protected final void bindServlet(String pattern, Class<? extends HttpServlet> klass) {
    bind(klass);
    servletDefs.add(new ServletDef(klass, pattern));
  }

  protected final void bindWithTransactionalMethodProbes(final Class<?>... classes) {
    for (Class<?> clazz : classes) {
      bind(clazz);
    }
    bindInterceptor(subclassesMatcher(classes), Matchers.annotatedWith(Transactional.class), MethodProbingInterceptor.INSTANCE);
  }

  protected final void bindTransactionalMethodProbesInterceptorOnly(final Class<?>... classes) {
    bindInterceptor(subclassesMatcher(classes), Matchers.annotatedWith(Transactional.class), MethodProbingInterceptor.INSTANCE);
  }

  protected final void bindWithAllMethodProbes(final Class<?>... classes) {
    for (Class<?> clazz : classes) {
      bind(clazz);
    }
    bindInterceptor(subclassesMatcher(classes), Matchers.any(), MethodProbingInterceptor.INSTANCE);
  }

  protected final void bindAllMethodProbesInterceptorOnly(final Class<?>... classes) {
    bindInterceptor(subclassesMatcher(classes), Matchers.any(), MethodProbingInterceptor.INSTANCE);
  }

  protected final void setDefaultFreeMarkerLayout(String layout) {
    defaultFreemarkerLayout = layout;
  }

  @Named("defaultFreeMarkerLayout")
  @Provides
  protected final String defaultFreeMarkerLayout() {
    return defaultFreemarkerLayout;
  }

  protected final void bindDataSourceAndEntityManagerAccessor(Class<?>... entities) {
    bindDefaultDataSource();
    bindDefaultEntityManagerAccessor(entities);
  }

  protected final void bindDataSourceAndEntityManagerAccessor(String name, Class<? extends Annotation> ann, Class<?>... entities) {
    bindDataSource(name, ann);
    bindEntityManagerAccessor(name, ann, entities);
  }

  protected final void bindDefaultEntityManagerAccessor(Class<?>... entities) {
    bindEntityManagerAccessor("default-db", Default.class, entities);
  }

  protected final void bindEntityManagerAccessor(String name, final Class<? extends Annotation> ann, Class<?>... entities) {
    bind(EntityManagerFactory.class).annotatedWith(ann)
    .toProvider(Providers.guicify(hibernateAccessorProvider(name, ann, entities)))
    .in(Scopes.SINGLETON);

    final Provider<EntityManagerFactory> emfProvider = getProvider(Key.get(EntityManagerFactory.class, ann));
    final TxInterceptor tx = new TxInterceptor(emfProvider);

    bind(TxInterceptor.class).annotatedWith(ann).toInstance(tx);
    bindInterceptor(Matchers.any(), new TransactionalMatcher(ann), tx);

    bind(EntityManager.class).annotatedWith(ann)
    .toProvider(Providers.guicify(new Provider<EntityManager>() {
          @Override
          public EntityManager get() {
            return tx.currentEntityManager();
          }
        }));

    bind(ModelAccess.class).annotatedWith(ann)
    .toProvider(Providers.guicify(new Provider<ModelAccess>() {
          @Override
          public ModelAccess get() {
            return new ModelAccess(emfProvider);
          }
        }))
    .in(Scopes.SINGLETON);

    bind(CriteriaBuilder.class).annotatedWith(ann)
    .toProvider(
      Providers.guicify(new Provider<CriteriaBuilder>() {
          @Override
          public CriteriaBuilder get() {
            return emfProvider.get().getCriteriaBuilder();
          }
        }))
    .in(Scopes.SINGLETON);

    bind(PostCommitHooks.class).annotatedWith(ann)
    .toProvider(Providers.guicify(new Provider<PostCommitHooks>() {
          @Override
          public PostCommitHooks get() {
            return tx.currentPostCommitHooks();
          }
        }));
  }

  private Provider<EntityManagerFactory> hibernateAccessorProvider(
      final String name, final Class<? extends Annotation> ann, final Class<?>... entities) {
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

        for (Class<?> entity : entities) {
          cfg.addAnnotatedClass(entity);
        }

        cfg.setListeners("pre-load", new PreLoadEventListener[] { new GuicyHibernateLoader(injector), new DefaultPreLoadEventListener() });
        cfg.setDataSource(injector.getInstance(Key.get(DataSource.class, ann)));
        return cfg.buildEntityManagerFactory();
      }
    };
  }

  protected final void bindDataSource(String name, Class<? extends Annotation> ann) {
    bind(DataSource.class).annotatedWith(ann).toProvider(Providers.guicify(dataSourceProvider(name))).in(Scopes.SINGLETON);
  }

  protected final void bindDefaultDataSource() {
    bind(DataSource.class).annotatedWith(Default.class).toProvider(Providers.guicify(dataSourceProvider("default-db"))).in(Scopes.SINGLETON);
  }

  private Provider<DataSource> dataSourceProvider(final String name) {
    return new Provider<DataSource>() {
      private Settings settings;

      @Inject
      public void setSettings(Settings settings) {
        this.settings = settings;
      }

      @Override
      @SuppressWarnings({ "unchecked" })
      public DataSource get() {
        Properties c3p0Props = settings.subTree(name + ".c3p0");
        Properties dbcpProps = settings.subTree(name + ".dbcp");

        Preconditions.checkState(c3p0Props.isEmpty() || dbcpProps.isEmpty(), "Both c3p0 and dbcp settings are present");
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
      Providers.guicify(
        new Provider<ScheduledExecutorService>() {
          @Inject
          public Injector injector;

          @Override
          public ScheduledExecutorService get() {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            for (ScheduledTaskDef taskDef : taskDefs) {
              Runnable r = injector.getInstance(taskDef.klass);
              scheduler.scheduleAtFixedRate(r, (long) (taskDef.time * Math.random()), taskDef.time, taskDef.unit);
            }
            return Executors.unconfigurableScheduledExecutorService(scheduler);
          }
        }))
    .asEagerSingleton();
  }

  private void bindServlets() {
    bind(ServletDefs.class).toInstance(servletDefs);
  }

  private void bindGrizzlets() {
    bind(GrizzletDefs.class).toInstance(grizzletDefs);
  }

  protected final void schedulePeriodicTask(Class<? extends Runnable> task, long time, TimeUnit unit) {
    taskDefs.add(new ScheduledTaskDef(task, time, unit));
  }

  @Provides
  @Singleton
  protected Random random(SecureRandom seed) {
    return new Random(System.nanoTime() ^ (System.currentTimeMillis() << 32) ^ seed.nextLong());
  }

  @Provides
  @Singleton
  protected SecureRandom secureRandom() {
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
      if (l.histo != null) {
        ls.put(l.name, l.histo);
      }
    }
    return new StatsDumper(ls);
  }

  static class ServletDefs extends ArrayList<ServletDef> { }

  static class GrizzletDefs extends ArrayList<GrizzletDef> { }

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
}
