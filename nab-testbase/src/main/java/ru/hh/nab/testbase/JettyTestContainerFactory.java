package ru.hh.nab.testbase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import ru.hh.nab.starter.NabApplication;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class JettyTestContainerFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(JettyTestContainer.class);

  private static final ConcurrentMap<Class<? extends NabTestBase>, JettyTestContainer> INSTANCES = new ConcurrentHashMap<>();
  private static final String BASE_URI = "http://127.0.0.1";

  private final ApplicationContext applicationContext;
  private final NabApplication application;
  private final Class<? extends NabTestBase> testClass;

  JettyTestContainerFactory(ApplicationContext applicationContext, NabApplication application, Class<? extends NabTestBase> testClass) {
    this.applicationContext = applicationContext;
    this.application = application;
    this.testClass = testClass;
  }

  JettyTestContainer createTestContainer() {
    Class<? extends NabTestBase> baseClass = findMostGenericBaseClass(testClass);
    return INSTANCES.compute(baseClass, (key, value) -> {
      if (value != null) {
        LOGGER.info("Reusing JettyTestContainer: {}", value);
        return value;
      }
      LOGGER.info("Creating new JettyTestContainer...");
      return new JettyTestContainer(application, (WebApplicationContext) applicationContext, BASE_URI);
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
}
