package ru.hh.nab.starter;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.starter.server.jetty.JettyServer;
import ru.hh.nab.starter.server.jetty.JettyServerFactory;
import ru.hh.nab.starter.servlet.ServletConfig;

import java.lang.management.ManagementFactory;

public class NabApplicationContext extends AnnotationConfigWebApplicationContext {

  private volatile JettyServer jettyServer;

  private final ServletConfig servletConfig;

  public NabApplicationContext(ServletConfig servletConfig, Class<?>... primarySources) {
    this.servletConfig = servletConfig;
    register(primarySources);
    registerShutdownHook();
  }

  @Override
  protected void finishRefresh() {
    super.finishRefresh();
    startJettyServer();
    printStartupInfo();
  }

  private void startJettyServer() {
    JettyServer jettyServer = this.jettyServer;
    try {
      if (jettyServer == null) {
        FileSettings jettySettings = getBean(FileSettings.class);
        ThreadPool threadPool = getBean(ThreadPool.class);
        ResourceConfig resourceConfig = getBean(ResourceConfig.class);

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

  private void printStartupInfo() {
    AppMetadata appMetadata = getBean(AppMetadata.class);
    System.out.println(appMetadata.getStatus() + ", pid " + getCurrentPid());
  }

  private static String getCurrentPid() {
    return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
  }
}
