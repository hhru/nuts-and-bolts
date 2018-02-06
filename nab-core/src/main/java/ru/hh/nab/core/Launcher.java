package ru.hh.nab.core;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.context.ApplicationContext;
import ru.hh.nab.core.filters.RequestIdLoggingFilter;
import ru.hh.nab.core.filters.ResourceNameLoggingFilter;
import ru.hh.nab.core.jersey.FilteredXmlElementProvider;
import ru.hh.nab.core.jersey.FilteredXmlListElementProvider;
import ru.hh.nab.core.jersey.FilteredXmlRootElementProvider;
import ru.hh.nab.core.jetty.JettyFactory;
import ru.hh.nab.core.util.FileSettings;

import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;

import static java.text.MessageFormat.format;

public abstract class Launcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(Launcher.class);

  protected static void doMain(ApplicationContext context) {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    try {
      startApplication(context);
    } catch (Exception e) {
      LOGGER.error("Failed to start, shutting down", e);
      System.err.println(format("[{0}] Failed to start, shutting down: {1}", LocalDateTime.now(), e.getMessage()));
      System.exit(1);
    }
  }

  public static int startApplication(ApplicationContext context) throws Exception {
    final FileSettings settings = context.getBean(FileSettings.class);
    final Server jettyServer = JettyFactory.create(settings, createJerseyServlet(context), context.getBean(ThreadPool.class));
    configureFilters(jettyServer, context);

    final int port = startJettyServer(jettyServer);
    AppMetadata appMetadata = context.getBean(AppMetadata.class);
    System.out.println(appMetadata.getStatus() + ", pid " + getCurrentPid() + ", listening to port " + port);

    return port;
  }

  private static void configureFilters(Server server, ApplicationContext applicationContext) {
    ServletContextHandler servletContextHandler = (ServletContextHandler) server.getHandler();
    servletContextHandler.addFilter(RequestIdLoggingFilter.class, "/*", EnumSet.allOf(DispatcherType.class));

    if (applicationContext.containsBean("cacheFilter")) {
      FilterHolder cacheFilter = applicationContext.getBean("cacheFilter", FilterHolder.class);
      if (cacheFilter.isInstance()) {
        servletContextHandler.addFilter(cacheFilter, "/*", EnumSet.allOf(DispatcherType.class));
      }
    }
  }

  private static Servlet createJerseyServlet(ApplicationContext context) {
    ResourceConfig resourceConfig = new ResourceConfig();
    resourceConfig.property("contextConfig", context);
    context.getBeansWithAnnotation(javax.ws.rs.Path.class)
        .forEach((name, resource) -> resourceConfig.register(resource));

    resourceConfig.register(FilteredXmlRootElementProvider.App.class);
    resourceConfig.register(FilteredXmlRootElementProvider.General.class);
    resourceConfig.register(FilteredXmlRootElementProvider.Text.class);
    resourceConfig.register(FilteredXmlElementProvider.App.class);
    resourceConfig.register(FilteredXmlElementProvider.General.class);
    resourceConfig.register(FilteredXmlElementProvider.Text.class);
    resourceConfig.register(FilteredXmlListElementProvider.App.class);
    resourceConfig.register(FilteredXmlListElementProvider.General.class);
    resourceConfig.register(FilteredXmlListElementProvider.Text.class);
    resourceConfig.register(new ResourceNameLoggingFilter());
    return new ServletContainer(resourceConfig);
  }

  private static int startJettyServer(Server jettyServer) throws Exception {
    jettyServer.start();
    return ((ServerConnector) Arrays.stream(jettyServer.getConnectors())
        .filter(a -> a instanceof ServerConnector).findFirst().get())
        .getLocalPort();
  }

  private static String getCurrentPid() {
    return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
  }
}
