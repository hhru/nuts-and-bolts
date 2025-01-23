package ru.hh.nab.web.starter.jetty;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.webapp.Configuration;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.env.Environment;
import ru.hh.nab.web.jetty.security.SecurityConfiguration;
import ru.hh.nab.web.jetty.session.SessionConfiguration;
import ru.hh.nab.web.starter.servlet.RegistrationValidator;

public class NabJettyWebServerFactoryCustomizer implements WebServerFactoryCustomizer<JettyServletWebServerFactory> {

  public static final String SERVER_SERVLET_SESSION_ENABLED_PROPERTY = "server.servlet.session.enabled";
  public static final String SERVER_SERVLET_SECURITY_ENABLED_PROPERTY = "server.servlet.security.enabled";

  public static final boolean DEFAULT_SERVER_SERVLET_SESSION_ENABLED = true;
  public static final boolean DEFAULT_SERVER_SERVLET_SECURITY_ENABLED = true;

  private final ListableBeanFactory beanFactory;
  private final MonitoredQueuedThreadPoolFactory monitoredQueuedThreadPoolFactory;
  private final ServerProperties serverProperties;
  private final Environment environment;
  private final List<Configuration> webAppConfigurations;

  public NabJettyWebServerFactoryCustomizer(
      ListableBeanFactory beanFactory,
      MonitoredQueuedThreadPoolFactory monitoredQueuedThreadPoolFactory,
      ServerProperties serverProperties,
      Environment environment,
      List<Configuration> webAppConfigurations
  ) {
    this.beanFactory = beanFactory;
    this.monitoredQueuedThreadPoolFactory = monitoredQueuedThreadPoolFactory;
    this.serverProperties = serverProperties;
    this.environment = environment;
    this.webAppConfigurations = webAppConfigurations;
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
        new SessionConfiguration(environment.getProperty(
            SERVER_SERVLET_SESSION_ENABLED_PROPERTY,
            Boolean.class,
            DEFAULT_SERVER_SERVLET_SESSION_ENABLED
        ))
    );
    webAppConfigurations.add(
        new SecurityConfiguration(environment.getProperty(
            SERVER_SERVLET_SECURITY_ENABLED_PROPERTY,
            Boolean.class,
            DEFAULT_SERVER_SERVLET_SECURITY_ENABLED
        ))
    );
    webAppConfigurations.addAll(this.webAppConfigurations);
    factory.addConfigurations(webAppConfigurations.toArray(Configuration[]::new));
  }
}
