package ru.hh.nab.core;

import static java.text.MessageFormat.format;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.context.ApplicationContext;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.core.jetty.JettyFactory;
import ru.hh.nab.core.servlet.DefaultServletConfig;
import ru.hh.nab.core.servlet.ServletConfig;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.Arrays;

public class NabApplication {
  private static final Logger LOGGER = LoggerFactory.getLogger(NabApplication.class);

  public static ApplicationContext run(Class<?>... primarySources) {
    return run(new DefaultServletConfig(), primarySources);
  }

  public static ApplicationContext run(ServletConfig servletConfig, Class<?>... primarySources) {
    registerSlf4JHandler();
    NabApplicationContext context = null;
    try {
      context = createApplicationContext(primarySources);
      int port = startJettyServer(context, servletConfig);
      printApplicationStatus(context, port);
    } catch (Exception e) {
      logErrorAndExit(e);
    }
    return context;
  }

  private static NabApplicationContext createApplicationContext(Class<?>... primarySources) {
    final NabApplicationContext context = new NabApplicationContext();
    context.register(primarySources);
    context.refresh();
    return context;
  }

  public static int startJettyServer(ApplicationContext context, ServletConfig servletConfig) throws Exception {
    final FileSettings settings = context.getBean(FileSettings.class);
    final ServletContainer mainServlet = new ServletContainer(servletConfig.createResourceConfig(context));
    final Server jettyServer = JettyFactory.create(settings, context.getBean(ThreadPool.class), mainServlet, servletConfig.getServletMapping());
    servletConfig.configureServletContext((ServletContextHandler) jettyServer.getHandler(), context);
    int port = startJettyServer(jettyServer);

    // todo: integrate start of jetty server into spring life cycle
    if (context instanceof NabApplicationContext) {
      NabApplicationContext nabApplicationContext = (NabApplicationContext) context;
      nabApplicationContext.setServer(jettyServer);
    }

    return port;
  }

  static void registerSlf4JHandler() {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  private static int startJettyServer(Server jettyServer) throws Exception {
    jettyServer.start();
    return ((ServerConnector) Arrays.stream(jettyServer.getConnectors())
        .filter(a -> a instanceof ServerConnector).findFirst().get())
        .getLocalPort();
  }

  private static void printApplicationStatus(ApplicationContext context, int port) {
    AppMetadata appMetadata = context.getBean(AppMetadata.class);
    System.out.println(appMetadata.getStatus() + ", pid " + getCurrentPid() + ", listening to port " + port);
  }

  private static void logErrorAndExit(Exception e) {
    LOGGER.error("Failed to start, shutting down", e);
    System.err.println(format("[{0}] Failed to start, shutting down: {1}", LocalDateTime.now(), e.getMessage()));
    System.exit(1);
  }

  private static String getCurrentPid() {
    return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
  }
}
