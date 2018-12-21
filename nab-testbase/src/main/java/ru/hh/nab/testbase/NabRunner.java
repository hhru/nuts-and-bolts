package ru.hh.nab.testbase;

import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.runners.model.InitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.BootstrapContext;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DefaultBootstrapContext;
import org.springframework.test.context.web.WebTestContextBootstrapper;
import org.springframework.web.context.WebApplicationContext;
import ru.hh.nab.testbase.spring.NabTestContext;

public class NabRunner extends SpringJUnit4ClassRunner {

  private static final ThreadLocal<NabRunner> CURRENT_INSTANCE = new ThreadLocal<>();

  public NabRunner(Class<?> clazz) throws InitializationError {
    super(clazz);
    CURRENT_INSTANCE.set(this);
  }

  private static TestContextManager getManager() {
    return CURRENT_INSTANCE.get().getTestContextManager();
  }

  /**
   * we lose ability to use {@link org.springframework.test.context.BootstrapWith}
   * @param clazz testClass
   * @return testContextManager with {@link NabBootstrapper}
   */
  @Override
  protected TestContextManager createTestContextManager(Class<?> clazz) {
    return new TestContextManager(new NabBootstrapper(new DefaultBootstrapContext(clazz, new DefaultCacheAwareContextLoaderDelegate())));
  }

  public static class NabBootstrapper extends WebTestContextBootstrapper {

    public NabBootstrapper(BootstrapContext bootstrapContext) {
      setBootstrapContext(bootstrapContext);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(NabBootstrapper.class);

    private static final ConcurrentMap<Class<? extends NabTestBase>, NabTestContext.PortHolder> PORTS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Class<? extends NabTestBase>, JettyTestContainer> SERVERS = new ConcurrentHashMap<>();

    public NabTestContext.PortHolder getPort(Class<? extends NabTestBase> testClass) {
      Class<? extends NabTestBase> baseClass = findMostGenericBaseClass(testClass);
      return PORTS.computeIfAbsent(baseClass, cls -> new NabTestContext.PortHolder());
    }

    @Override
    protected CacheAwareContextLoaderDelegate getCacheAwareContextLoaderDelegate() {
      return new NabCacheAwareLoaderDelegate();
    }

    public JettyTestContainer getJetty(NabTestBase testInstance, WebApplicationContext webApplicationContext) {
      Class<? extends NabTestBase> baseClass = findMostGenericBaseClass(testInstance.getClass());
      return SERVERS.compute(baseClass, (key, value) -> {
        if (value != null) {
          LOGGER.debug("Reusing JettyTestContainer: {}", value);
          return value;
        }
        LOGGER.info("Creating new JettyTestContainer...");
        return new JettyTestContainer(testInstance.getApplication(), webApplicationContext, getPort(baseClass));
      });
    }

    private static Class<? extends NabTestBase> findMostGenericBaseClass(Class<? extends NabTestBase> clazz) {
      Class<? extends NabTestBase> current = clazz;
      while (true) {
        try {
          current.getDeclaredMethod("getApplication");
          return current;
        } catch (NoSuchMethodException e) {
          current = current.getSuperclass().asSubclass(NabTestBase.class);
        }
      }
    }

    @Override
    public TestContext buildTestContext() {
      return new NabTestContext(super.buildTestContext(), this);
    }

    @Override
    protected MergedContextConfiguration processMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
      MergedContextConfiguration overridenConfig = new MergedContextConfiguration(mergedConfig.getTestClass(), mergedConfig.getLocations(),
        ArrayUtils.add(mergedConfig.getClasses(), JettyTestConfig.class), mergedConfig.getContextInitializerClasses(),
        mergedConfig.getActiveProfiles(), mergedConfig.getContextLoader());
      return super.processMergedContextConfiguration(overridenConfig);
    }
  }

  /**
   * Beans must be prototype to be able return different values in different contexts
   * TODO looks like custom spring scope
   */
  public static class JettyTestConfig {

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    NabTestContext testContext() {
      return (NabTestContext) getManager().getTestContext();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    String jettyBaseUrl(NabTestContext testContext) throws UnknownHostException {
      return JettyTestContainer.getServerAddress(testContext.getPortHolder().getPort()).toString();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    JettyTestContainer jettyTestContainer(WebApplicationContext applicationContext, NabTestContext testContext) {
      return testContext.getJetty(applicationContext);
    }
  }
}
