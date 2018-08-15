package ru.hh.nab.starter.server.jetty;

import static java.util.Optional.ofNullable;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.starter.servlet.JerseyServletContextInitializer;
import ru.hh.nab.starter.servlet.ServletConfig;

import javax.servlet.Servlet;

public final class JettyServerFactory {

  public static JettyServer create(FileSettings fileSettings,
                                   ThreadPool threadPool,
                                   ResourceConfig resourceConfig,
                                   ServletConfig servletConfig,
                                   JerseyServletContextInitializer servletContextInitializer) {

    FileSettings jettySettings = fileSettings.getSubSettings("jetty");
    ServletContainer servletContainer = createServletContainer(resourceConfig, servletConfig);
    ServletContextHandler contextHandler = createWebAppContextHandler(servletContainer, servletConfig, jettySettings, servletContextInitializer);
    return new JettyServer(threadPool, jettySettings, contextHandler);
  }

  private static ServletContextHandler createWebAppContextHandler(Servlet mainServlet,
                                                                  ServletConfig servletConfig,
                                                                  FileSettings jettySettings,
                                                                  JerseyServletContextInitializer servletContextInitializer) {
    boolean sessionEnabled = ofNullable(jettySettings.getBoolean("session-manager.enabled")).orElse(false);
    final ServletContextHandler contextHandler = new JettyWebAppContext(servletContextInitializer, sessionEnabled);
    final ServletHolder servletHolder = new ServletHolder("mainServlet", mainServlet);

    final ServletHandler servletHandler = new ServletHandler();
    servletHandler.addServletWithMapping(servletHolder, servletConfig.getServletMapping());
    contextHandler.setServletHandler(servletHandler);
    return contextHandler;
  }

  public static ThreadPool createJettyThreadPool(FileSettings jettySettings) throws Exception {
    int minThreads = ofNullable(jettySettings.getInteger("minThreads")).orElse(4);
    int maxThreads = ofNullable(jettySettings.getInteger("maxThreads")).orElse(12);
    int idleTimeoutMs = ofNullable(jettySettings.getInteger("threadPoolIdleTimeoutMs")).orElse(60_000);
    QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads, idleTimeoutMs, new BlockingArrayQueue<>(maxThreads));
    threadPool.start();
    return threadPool;
  }

  private static ServletContainer createServletContainer(ResourceConfig resourceConfig, ServletConfig servletConfig) {
    servletConfig.registerResources(resourceConfig);
    return new ServletContainer(resourceConfig);
  }

  private JettyServerFactory() {
  }
}
