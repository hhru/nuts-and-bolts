package ru.hh.nab.starter;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.starter.jersey.DefaultResourceConfig;
import ru.hh.nab.starter.server.jetty.JettyServer;
import ru.hh.nab.starter.server.jetty.JettyServerFactory;
import ru.hh.nab.starter.servlet.ServletConfig;

public final class NabApplicationContext extends AnnotationConfigWebApplicationContext {

  private volatile JettyServer jettyServer;

  private final ServletConfig servletConfig;

  NabApplicationContext(ServletConfig servletConfig, Class<?>... primarySources) {
    this.servletConfig = servletConfig;
    register(primarySources);
    registerShutdownHook();
  }

  @Override
  protected void finishRefresh() {
    super.finishRefresh();
    startJettyServer();
  }

  private void startJettyServer() {
    JettyServer jettyServer = this.jettyServer;
    try {
      if (jettyServer == null) {
        FileSettings jettySettings = getBean(FileSettings.class);
        ThreadPool threadPool = getBean(ThreadPool.class);
        ResourceConfig resourceConfig = new DefaultResourceConfig();

        this.jettyServer = JettyServerFactory.create(jettySettings, threadPool, resourceConfig, servletConfig, (contextHandler) -> {
          configureServletContext(contextHandler, this, servletConfig);
          setServletContext(contextHandler.getServletContext());
        });

        this.jettyServer.start();
      }
    } catch (Throwable t) {
      throw new ApplicationContextException("Unable to start application server", t);
    }
  }

  public static void configureServletContext(ServletContextHandler handler, ApplicationContext applicationContext, ServletConfig servletConfig) {
    handler.getServletContext().setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext);
    servletConfig.configureServletContext(handler, applicationContext);
  }

  boolean isServerRunning() {
    return jettyServer.isRunning();
  }
}
