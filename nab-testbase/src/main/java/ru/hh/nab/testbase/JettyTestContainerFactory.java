package ru.hh.nab.testbase;

import org.eclipse.jetty.util.thread.ThreadPool;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.glassfish.jersey.test.spi.TestHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.starter.NabApplicationContext.configureServletContext;
import ru.hh.nab.starter.server.jetty.JettyServer;
import ru.hh.nab.starter.server.jetty.JettyServerFactory;
import ru.hh.nab.starter.servlet.ServletConfig;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class JettyTestContainerFactory implements TestContainerFactory {

  private static final ConcurrentMap<Class<? extends NabJerseyTestBase>, TestContainer> INSTANCES = new ConcurrentHashMap<>();

  private final WebApplicationContext springContext;
  private final ServletConfig servletConfig;
  private final Class<? extends NabJerseyTestBase> testClass;

  JettyTestContainerFactory(WebApplicationContext context, ServletConfig servletConfig, Class<? extends NabJerseyTestBase> testClass) {
    this.springContext = context;
    this.servletConfig = servletConfig;
    this.testClass = testClass;
  }

  @Override
  public TestContainer create(URI baseUri, DeploymentContext deploymentContext) {
    Class<? extends NabJerseyTestBase> baseClass = findMostGenericBaseClass(testClass);
    return INSTANCES.computeIfAbsent(baseClass,
        key -> new JettyServerTestContainer(baseUri, servletConfig, deploymentContext, springContext));
  }

  private static Class<? extends NabJerseyTestBase> findMostGenericBaseClass(Class<? extends NabJerseyTestBase> clazz) {
    Class<? extends NabJerseyTestBase> current = clazz;
    while (true) {
      try {
        current.getDeclaredMethod("getServletConfig");
        return current;
      } catch (NoSuchMethodException e) {
        current = current.getSuperclass().asSubclass(NabJerseyTestBase.class);
      }
    }
  }

  private static class JettyServerTestContainer implements TestContainer {
    private static final Logger LOGGER = LoggerFactory.getLogger(JettyServerTestContainer.class);

    private final JettyServer jettyServer;
    private URI baseUri;

    JettyServerTestContainer(URI baseUri, ServletConfig servletConfig, DeploymentContext deploymentContext, WebApplicationContext springContext) {
      final URI base = UriBuilder.fromUri(baseUri).path(deploymentContext.getContextPath()).build();

      if (!"/".equals(base.getRawPath())) {
        throw new TestContainerException(
            String.format("Cannot deploy on %s. Jetty HTTP container only supports deployment on root path.", base.getRawPath()));
      }

      this.baseUri = base;

      LOGGER.info("Creating JettyServerTestContainer configured at the base URI " + TestHelper.zeroPortToAvailablePort(baseUri));

      final FileSettings fileSettings = springContext.getBean(FileSettings.class);
      final ThreadPool threadPool = springContext.getBean(ThreadPool.class);

      jettyServer = JettyServerFactory.create(
          fileSettings,
          threadPool,
          deploymentContext.getResourceConfig(),
          servletConfig,
          (contextHandler) -> configureServletContext(contextHandler, springContext, servletConfig));
    }

    @Override
    public ClientConfig getClientConfig() {
      return null;
    }

    @Override
    public URI getBaseUri() {
      return baseUri;
    }

    @Override
    public void start() {
      if (jettyServer.isRunning()) {
        LOGGER.warn("Ignoring start request - JettyServerTestContainer is already started.");
        return;
      }

      LOGGER.info("Starting JettyServerTestContainer...");

      jettyServer.start();
      baseUri = UriBuilder.fromUri(baseUri).port(jettyServer.getPort()).build();

      LOGGER.info("Started JettyServerTestContainer at the base URI " + baseUri);
    }

    @Override
    public void stop() {
    }
  }
}
