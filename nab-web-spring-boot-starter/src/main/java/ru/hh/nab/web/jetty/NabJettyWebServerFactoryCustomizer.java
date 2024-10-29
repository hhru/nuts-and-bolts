package ru.hh.nab.web.jetty;

import org.eclipse.jetty.util.thread.ThreadPool;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import ru.hh.nab.web.servlet.RegistrationValidator;

public class NabJettyWebServerFactoryCustomizer implements WebServerFactoryCustomizer<JettyServletWebServerFactory> {

  private final ListableBeanFactory beanFactory;
  private final MonitoredQueuedThreadPoolFactory monitoredQueuedThreadPoolFactory;
  private final ServerProperties serverProperties;

  public NabJettyWebServerFactoryCustomizer(
      ListableBeanFactory beanFactory,
      MonitoredQueuedThreadPoolFactory monitoredQueuedThreadPoolFactory,
      ServerProperties serverProperties
  ) {
    this.beanFactory = beanFactory;
    this.monitoredQueuedThreadPoolFactory = monitoredQueuedThreadPoolFactory;
    this.serverProperties = serverProperties;
  }

  @Override
  public void customize(JettyServletWebServerFactory factory) {
    configureThreadPool(factory);
    configureWebAppConfigurations(factory);
  }

  private void configureThreadPool(JettyServletWebServerFactory factory) {
    ThreadPool originalThreadPool = factory.getThreadPool();
    ThreadPool newThreadPool = monitoredQueuedThreadPoolFactory.create(serverProperties);
    if (!(originalThreadPool.getClass().isAssignableFrom(newThreadPool.getClass()))) {
      throw new IllegalStateException(
          ("Jetty thread pool created by nab should override thread pool created by spring-boot. But thread pool created by nab has type %s and " +
              "thread pool created by spring-boot has type %s")
              .formatted(newThreadPool.getClass(), originalThreadPool.getClass())
      );
    }
    factory.setThreadPool(newThreadPool);
  }

  private void configureWebAppConfigurations(JettyServletWebServerFactory factory) {
    factory.addInitializers(new RegistrationValidator(beanFactory));
  }
}
