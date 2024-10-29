package ru.hh.nab.web.jetty;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.webapp.Configuration;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import ru.hh.nab.starter.server.jetty.SecurityConfiguration;
import ru.hh.nab.starter.server.jetty.SessionConfiguration;
import ru.hh.nab.web.ExtendedServerProperties;
import ru.hh.nab.web.servlet.RegistrationValidator;

public class NabJettyWebServerFactoryCustomizer implements WebServerFactoryCustomizer<JettyServletWebServerFactory> {

  private final ListableBeanFactory beanFactory;
  private final MonitoredQueuedThreadPoolFactory monitoredQueuedThreadPoolFactory;
  private final ServerProperties serverProperties;
  private final ExtendedServerProperties extendedServerProperties;

  public NabJettyWebServerFactoryCustomizer(
      ListableBeanFactory beanFactory,
      MonitoredQueuedThreadPoolFactory monitoredQueuedThreadPoolFactory,
      ServerProperties serverProperties,
      ExtendedServerProperties extendedServerProperties
  ) {
    this.beanFactory = beanFactory;
    this.monitoredQueuedThreadPoolFactory = monitoredQueuedThreadPoolFactory;
    this.serverProperties = serverProperties;
    this.extendedServerProperties = extendedServerProperties;
  }

  @Override
  public void customize(JettyServletWebServerFactory factory) {
    configureThreadPool(factory);
    configureServletContextInitializers(factory);
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

  private void configureServletContextInitializers(JettyServletWebServerFactory factory) {
    factory.addInitializers(new RegistrationValidator(beanFactory));
  }

  private void configureWebAppConfigurations(JettyServletWebServerFactory factory) {
    List<Configuration> webAppConfigurations = new ArrayList<>();
    webAppConfigurations.add(
        new SessionConfiguration(extendedServerProperties.getServlet().getSession().isEnabled())
    );
    webAppConfigurations.add(
        new SecurityConfiguration(extendedServerProperties.getServlet().getSecurity().isEnabled())
    );
    factory.addConfigurations(webAppConfigurations.toArray(Configuration[]::new));
  }
}
