package ru.hh.nab.core.jetty;

import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import ru.hh.jetty.HHServerConnector;
import ru.hh.jetty.RequestLogger;
import ru.hh.jetty.RequestWithCacheLogger;
import ru.hh.nab.core.util.FileSettings;

import javax.servlet.Servlet;
import java.lang.management.ManagementFactory;
import java.util.Optional;

public class JettyFactory {

  public static Server create(FileSettings settings, ThreadPool threadPool, Servlet mainServlet, String servletMapping) {
    final Handler mainHandler = createMainHandler(mainServlet, servletMapping);
    return createServer(settings.getSubSettings("jetty"), mainHandler, threadPool);
  }

  private static Handler createMainHandler(final Servlet mainServlet, String servletMapping) {
    final ServletHolder servletHolder = new ServletHolder("mainServlet", mainServlet);

    final ServletHandler servletHandler = new ServletHandler();
    servletHandler.addServletWithMapping(servletHolder, servletMapping);

    final ServletContextHandler servletContextHandler = new ServletContextHandler();
    servletContextHandler.setServletHandler(servletHandler);
    return servletContextHandler;
  }

  private static Server createServer(FileSettings jettySettings, Handler mainHandler, ThreadPool threadPool) {
    final Server server = new Server(threadPool);
    configureServerConnector(server, jettySettings);
    configureMBeanContainer(server);

    boolean httpCacheEnabled = jettySettings.getBoolean("http.cache.sizeInMB") != null;
    server.setRequestLog(httpCacheEnabled ? new RequestWithCacheLogger() : new RequestLogger());

    server.setHandler(mainHandler);

    server.setStopAtShutdown(true);
    server.setStopTimeout(Optional.ofNullable(jettySettings.getInteger("stopTimeoutMs")).orElse(5_000));

    return server;
  }

  public static ThreadPool createJettyThreadPool(FileSettings jettySettings) throws Exception {
    int minThreads = Optional.ofNullable(jettySettings.getInteger("minThreads")).orElse(10);
    int maxThreads = Optional.ofNullable(jettySettings.getInteger("maxThreads")).orElse(20);
    int idleTimeoutMs = 60_000;
    QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads, idleTimeoutMs);
    threadPool.start();
    return threadPool;
  }

  private static void configureServerConnector(Server server, FileSettings jettySettings) {
    ServerConnector serverConnector = new HHServerConnector(
        server,
        Optional.ofNullable(jettySettings.getInteger("acceptors")).orElse(-1),
        Optional.ofNullable(jettySettings.getInteger("selectors")).orElse(-1),
        createHttpConnectionFactory());

    serverConnector.setPort(jettySettings.getInteger("port"));
    serverConnector.setIdleTimeout(Optional.ofNullable(jettySettings.getInteger("connectionIdleTimeoutMs")).orElse(3_000));
    serverConnector.setAcceptQueueSize(Optional.ofNullable(jettySettings.getInteger("acceptQueueSize")).orElse(50));

    server.addConnector(serverConnector);
  }

  private static void configureMBeanContainer(Server server) {
    final MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
    server.addEventListener(mbContainer);
    server.addBean(mbContainer);
  }

  private static HttpConnectionFactory createHttpConnectionFactory() {
    final HttpConfiguration httpConfiguration = new HttpConfiguration();
    httpConfiguration.setSecurePort(8443);
    httpConfiguration.setOutputBufferSize(65536);
    httpConfiguration.setRequestHeaderSize(16384);
    httpConfiguration.setResponseHeaderSize(65536);
    httpConfiguration.setSendServerVersion(false);
    httpConfiguration.setBlockingTimeout(5000);
    return new HttpConnectionFactory(httpConfiguration);
  }

  private JettyFactory() {
  }
}
