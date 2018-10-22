package ru.hh.nab.testbase;

import javax.servlet.ServletContextEvent;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.starter.server.jetty.JettyServer;
import ru.hh.nab.starter.server.jetty.JettyServerFactory;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import ru.hh.nab.starter.NabServletContextConfig;

final class JettyTestContainerFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(JettyTestContainer.class);

  private static final ConcurrentMap<Class<? extends NabTestBase>, JettyTestContainer> INSTANCES = new ConcurrentHashMap<>();
  private static final String BASE_URI = "http://127.0.0.1";

  private final ApplicationContext applicationContext;
  private final NabServletContextConfig servletConfig;
  private final Class<? extends NabTestBase> testClass;

  JettyTestContainerFactory(ApplicationContext applicationContext, NabServletContextConfig servletConfig, Class<? extends NabTestBase> testClass) {
    this.applicationContext = applicationContext;
    this.servletConfig = servletConfig;
    this.testClass = testClass;
  }

  JettyTestContainer createTestContainer() {
    Class<? extends NabTestBase> baseClass = findMostGenericBaseClass(testClass);
    JettyTestContainer testContainer = INSTANCES.computeIfAbsent(baseClass,
        key -> new JettyTestContainer(servletConfig, (WebApplicationContext) applicationContext));
    testContainer.start();
    return testContainer;
  }

  private static Class<? extends NabTestBase> findMostGenericBaseClass(Class<? extends NabTestBase> clazz) {
    Class<? extends NabTestBase> current = clazz;
    while (true) {
      try {
        current.getDeclaredMethod("getServletConfig");
        return current;
      } catch (NoSuchMethodException e) {
        current = current.getSuperclass().asSubclass(NabTestBase.class);
      }
    }
  }

  static class JettyTestContainer {
    private final JettyServer jettyServer;
    private URI baseUri;

    JettyTestContainer(NabServletContextConfig servletContextConfig, WebApplicationContext applicationContext) {
      this.baseUri = UriBuilder.fromUri(BASE_URI).build();

      LOGGER.info("Creating JettyTestContainer...");

      final FileSettings fileSettings = applicationContext.getBean(FileSettings.class);
      final ThreadPool threadPool = applicationContext.getBean(ThreadPool.class);

      jettyServer = JettyServerFactory.create(fileSettings, threadPool, webApp -> {
        NabServletContextConfig.TestBridge publicBridge = servletContextConfig.getTestBridge();
        publicBridge.preConfigureWebApp(webApp, applicationContext);
        webApp.addEventListener(new ContextLoaderListener(applicationContext) {
          @Override
          public void contextInitialized(ServletContextEvent event) {
            super.contextInitialized(event);
            publicBridge.onWebAppStarted(event.getServletContext(), applicationContext);
          }
        });
      });
    }

    void start() {
      if (jettyServer.isRunning()) {
        LOGGER.warn("Ignoring start request - JettyTestContainer is already started.");
        return;
      }

      LOGGER.info("Starting JettyTestContainer...");

      jettyServer.start();
      baseUri = UriBuilder.fromUri(baseUri).port(jettyServer.getPort()).build();

      LOGGER.info("Started JettyTestContainer at the base URI {}", baseUri);
    }

    String getBaseUrl() {
      return baseUri.toString();
    }

    int getPort() {
      return jettyServer.getPort();
    }
  }
}
