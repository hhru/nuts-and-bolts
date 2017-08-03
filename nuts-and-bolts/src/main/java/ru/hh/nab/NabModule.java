package ru.hh.nab;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.AnnotatedElement;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Executors;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import com.sun.jersey.spi.container.ResourceFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;
import mx4j.tools.adaptor.http.HttpAdaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jdebug.jersey1.provider.DebugResourceRequestFilter;
import ru.hh.jdebug.jersey1.provider.DebugResourceResponseFilter;
import ru.hh.metrics.StatsDSender;
import ru.hh.nab.jersey.Concurrency;
import ru.hh.nab.health.limits.LeakDetector;
import ru.hh.nab.health.limits.Limit;
import ru.hh.nab.health.limits.Limits;
import ru.hh.nab.health.monitoring.Dumpable;
import ru.hh.nab.health.monitoring.MethodProbingInterceptor;
import ru.hh.nab.health.monitoring.Probe;
import ru.hh.nab.health.monitoring.StatsDumper;
import ru.hh.nab.jersey.ConcurrentJerseyMethodInterceptor;
import ru.hh.nab.jersey.JerseyHttpServlet;
import ru.hh.nab.jersey.RequestUrlFilter;
import ru.hh.nab.scopes.RequestScope;
import ru.hh.nab.scopes.ThreadLocalScope;
import ru.hh.nab.scopes.ThreadLocalScoped;
import ru.hh.nab.security.Permissions;
import ru.hh.nab.security.SecureInterceptor;
import ru.hh.nab.security.SecureMatcher;
import ru.hh.nab.security.UnauthorizedExceptionJerseyMapper;

@SuppressWarnings("UnusedDeclaration")
public abstract class NabModule extends AbstractModule {
  private static final MBeanServer SERVER = ManagementFactory.getPlatformMBeanServer();
  private final List<ScheduledTaskDef> taskDefs = new ArrayList<>();
  private final List<ResourceFilterFactory> jerseyPreFilterFactories = new ArrayList<>();
  private final List<ResourceFilterFactory> jerseyPostFilterFactories = new ArrayList<>();

  private String defaultFreemarkerLayout = "nab/empty";

  @Override
  protected final void configure() {
    configureApp();
    bindScheduler();

    bindScope(ThreadLocalScoped.class, ThreadLocalScope.THREAD_LOCAL);

    bind(UnauthorizedExceptionJerseyMapper.class);
    bindInterceptor(Matchers.any(), new SecureMatcher(), new SecureInterceptor(getProvider(Permissions.class)));
    schedulePeriodicTask(StatsDumper.class, 10, TimeUnit.SECONDS);
    schedulePeriodicTask(LeakDetector.class, 10, TimeUnit.SECONDS);

    bindInterceptor(Matchers.any(), Matchers.annotatedWith(Probe.class), MethodProbingInterceptor.INSTANCE);

    ConcurrentJerseyMethodInterceptor concurrentJerseyMethodInterceptor = new ConcurrentJerseyMethodInterceptor();
    requestInjection(concurrentJerseyMethodInterceptor);
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(Concurrency.class), concurrentJerseyMethodInterceptor);
    install(new DebugModule());
    initializeDebugFilters();
  }

  protected abstract void configureApp();

  @Named("JerseyPreFilterFactories")
  @Provides
  @Singleton
  protected List<ResourceFilterFactory> jerseyPreFilters() {
    return jerseyPreFilterFactories;
  }

  @Named("JerseyPostFilterFactories")
  @Provides
  @Singleton
  protected List<ResourceFilterFactory> jerseyPostFilters() {
    return jerseyPostFilterFactories;
  }

  protected void addPreFilter(ResourceFilter filter) {
    addPreFilterFactory(am -> {
      return Collections.singletonList(filter);
    });
  }

  protected void addPostFilter(ResourceFilter filter) {
    addPostFilterFactory(am -> {
      return Collections.singletonList(filter);
    });
  }

  protected void addPreFilterFactory(ResourceFilterFactory filter) {
    jerseyPreFilterFactories.add(filter);
  }

  protected void addPostFilterFactory(ResourceFilterFactory filter) {
    jerseyPostFilterFactories.add(filter);
  }

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

  protected final void bindWithAnnotationMethodProbes(Class<? extends Annotation> annotation, Class<?>... classes) {
    for (Class<?> clazz : classes) {
      bind(clazz);
    }
    bindInterceptor(subclassesMatcher(classes), Matchers.annotatedWith(annotation), MethodProbingInterceptor.INSTANCE);
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
              scheduler.scheduleAtFixedRate(r, (long) (taskDef.time * Math.random()), taskDef.time, taskDef.unit);
            }
            return Executors.unconfigurableScheduledExecutorService(scheduler);
          }
        })
    .asEagerSingleton();
  }

  @Provides
  @Singleton
  protected HttpAdaptor httpAdaptor(Settings settings) {
    Properties mx4jProps = settings.subTree("mx4j");
    if ("true".equals(mx4jProps.getProperty("mode"))) {
      try {
        HttpAdaptor adaptor = new HttpAdaptor();
        ObjectName name = new ObjectName("Server:name=HttpAdaptor");
        SERVER.registerMBean(adaptor, name);
        adaptor.setPort(Integer.parseInt(mx4jProps.getProperty("port")));
        adaptor.setHost(mx4jProps.getProperty("host"));
        return adaptor;
      } catch (Exception ex) {
        Logger logger = LoggerFactory.getLogger(HttpAdaptor.class);
        logger.error("Can't create HttpAdaptor: " + ex);
        return null;
      }
    } else {
      return null;
    }
  }

  @Provides
  @Singleton
  private ScheduledExecutorService statsdScheduledExecutorService(@Named("serviceName") String serviceName) {
    ThreadFactory threadFactory = runnable -> {
      Thread thread = new Thread(runnable, serviceName + " statsd scheduled executor");
      thread.setDaemon(true);
      return thread;
    };
    return newSingleThreadScheduledExecutor(threadFactory);
  }

  @Provides
  @Singleton
  private StatsDClient statsDClient() {
    return new NonBlockingStatsDClient(null, "localhost", 8125, 10000);
  }

  @Provides
  @Singleton
  private StatsDSender statsDSender(ScheduledExecutorService scheduledExecutorService, StatsDClient statsDClient) {
    return new StatsDSender(statsDClient, scheduledExecutorService);
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

  @Named("controller_mdc_key")
  @Provides
  @Singleton
  protected String controllerMdcKey() {
    return RequestUrlFilter.CONTROLLER_MDC_KEY;
  }

  @Named("stack_outer_class_excluding")
  @Provides
  @Singleton
  protected String stackOuterClassExcluding() {
    return JerseyHttpServlet.class.getName();
  }

  @Named("stack_outer_method_excluding")
  @Provides
  @Singleton
  protected String stackOuterMethodExcluding() {
    return "service";
  }

  @Provides
  @Singleton
  protected AppMetadata getAppMetaData(@Named("serviceName") String serviceName) {
    return new AppMetadata(serviceName);
  }

  private void initializeDebugFilters() {
    addPreFilterFactory(new DebugResourceRequestFilter());
    addPostFilterFactory(new DebugResourceResponseFilter());
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
}
