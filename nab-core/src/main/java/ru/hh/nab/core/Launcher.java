package ru.hh.nab.core;

import java.lang.management.ManagementFactory;
import static java.text.MessageFormat.format;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.context.ApplicationContext;
import ru.hh.nab.core.jetty.JettyFactory;
import ru.hh.nab.core.servlet.DefaultServletConfig;
import ru.hh.nab.core.servlet.ServletConfig;
import ru.hh.nab.common.util.FileSettings;

public abstract class Launcher {
  private static final Logger LOGGER = LoggerFactory.getLogger(Launcher.class);

  protected static void doMain(ApplicationContext context) {
    doMain(context, new DefaultServletConfig());
  }

  protected static void doMain(ApplicationContext context, ServletConfig config) {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    try {
      startApplication(context, config);
    } catch (Exception e) {
      LOGGER.error("Failed to start, shutting down", e);
      System.err.println(format("[{0}] Failed to start, shutting down: {1}", LocalDateTime.now(), e.getMessage()));
      System.exit(1);
    }
  }

  public static int startApplication(ApplicationContext context, ServletConfig config) throws Exception {
    final FileSettings settings = context.getBean(FileSettings.class);
    final ServletContainer mainServlet = new ServletContainer(config.createResourceConfig(context));
    final Server jettyServer = JettyFactory.create(settings, context.getBean(ThreadPool.class), mainServlet, config.getServletMapping());
    config.configureServletContext((ServletContextHandler) jettyServer.getHandler(), context);
    final int port = startJettyServer(jettyServer);
    AppMetadata appMetadata = context.getBean(AppMetadata.class);
    System.out.println(appMetadata.getStatus() + ", pid " + getCurrentPid() + ", listening to port " + port);
    return port;
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
