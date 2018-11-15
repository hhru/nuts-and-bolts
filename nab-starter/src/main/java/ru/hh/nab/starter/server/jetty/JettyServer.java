package ru.hh.nab.starter.server.jetty;

import static java.util.Optional.ofNullable;
import javax.servlet.ServletContext;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jetty.HHServerConnector;
import ru.hh.jetty.RequestLogger;
import ru.hh.jetty.RequestWithCacheLogger;
import ru.hh.nab.common.properties.FileSettings;

import java.lang.management.ManagementFactory;
import java.util.Optional;

public final class JettyServer {
  private static final Logger LOGGER = LoggerFactory.getLogger(JettyServer.class);
  public static final String JETTY = "jetty";
  public static final String PORT = "port";
  public static final String JETTY_PORT = String.join(".", JETTY, PORT);

  private final FileSettings jettySettings;
  private final Server server;
  private final ServletContextHandler servletContextHandler;

  JettyServer(ThreadPool threadPool, FileSettings jettySettings, ServletContextHandler servletContextHandler) {
    this.jettySettings = jettySettings;

    server = new Server(threadPool);
    configureConnector();
    configureMBeanContainer();
    configureRequestLogger();
    configureStopTimeout();
    this.servletContextHandler = servletContextHandler;
    server.setHandler(servletContextHandler);
  }

  public void start() throws JettyServerException {
    try {
      server.start();
      server.setStopAtShutdown(true);

      LOGGER.info("Jetty started on port {}", getPort());
    } catch (Exception e) {
      stopSilently();
      String msg = ofNullable(jettySettings.getInteger("port")).filter(port -> port != 0).map(port -> ", port=" + port).orElse("");
      throw new JettyServerException("Unable to start Jetty server" + msg, e);
    }
  }

  public void stop() throws JettyServerException {
    try {
      server.stop();
    } catch (Exception e) {
      throw new JettyServerException("Unable to stop Jetty server", e);
    }
  }

  public int getPort() {
    Optional<ServerConnector> serverConnector = getServerConnector();
    if (!serverConnector.isPresent()) {
      LOGGER.warn("Unable to obtain port number - server connector is not present");
      return 0;
    }
    return serverConnector.get().getLocalPort();
  }

  public boolean isRunning() {
    return server.isRunning();
  }

  private void configureConnector() {
    ServerConnector serverConnector = new HHServerConnector(
        server,
        ofNullable(jettySettings.getInteger("acceptors")).orElse(1),
        ofNullable(jettySettings.getInteger("selectors")).orElse(1),
        createHttpConnectionFactory());

    serverConnector.setHost(jettySettings.getString("host"));
    serverConnector.setPort(jettySettings.getInteger(PORT));
    serverConnector.setIdleTimeout(ofNullable(jettySettings.getInteger("connectionIdleTimeoutMs")).orElse(3_000));
    serverConnector.setAcceptQueueSize(ofNullable(jettySettings.getInteger("acceptQueueSize")).orElse(50));

    server.addConnector(serverConnector);
  }

  private void configureMBeanContainer() {
    final MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
    server.addEventListener(mbContainer);
    server.addBean(mbContainer);
  }

  private void configureRequestLogger() {
    boolean httpCacheEnabled = jettySettings.getBoolean("http.cache.sizeInMB") != null;
    server.setRequestLog(httpCacheEnabled ? new RequestWithCacheLogger() : new RequestLogger());
  }

  private void configureStopTimeout() {
    server.setStopTimeout(ofNullable(jettySettings.getInteger("stopTimeoutMs")).orElse(5_000));
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

  private Optional<ServerConnector> getServerConnector() {
    Connector[] connectors = server.getConnectors();
    for (Connector connector: connectors) {
      if (connector instanceof ServerConnector) {
        return Optional.of((ServerConnector) connector);
      }
    }
    return Optional.empty();
  }

  private void stopSilently() {
    try {
      server.stop();
    } catch (Exception e) {
      // ignore
    }
  }

  public Server getServer() {
    return server;
  }

  public ServletContext getServletContext() {
    return servletContextHandler.getServletContext();
  }
}
