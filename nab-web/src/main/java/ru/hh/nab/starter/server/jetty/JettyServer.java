package ru.hh.nab.starter.server.jetty;

import jakarta.servlet.ServletContext;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.common.properties.FileSettings;
import ru.hh.nab.starter.exceptions.ConsulServiceException;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.PORT;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.STOP_TIMEOUT_SIZE;
import ru.hh.nab.starter.server.logging.StructuredRequestLogger;

public final class JettyServer {
  private static final Logger LOGGER = LoggerFactory.getLogger(JettyServer.class);

  private final FileSettings jettySettings;
  private final Server server;
  private final ServletContextHandler servletContextHandler;
  private final ContextHandlerCollection handlerCollection;

  JettyServer(FileSettings jettySettings, ServletContextHandler servletContextHandler) {
    this.jettySettings = jettySettings;
    server = new Server();
    configureRequestLogger();
    configureStopTimeout();
    this.servletContextHandler = servletContextHandler;
    handlerCollection = null;
    server.setHandler(servletContextHandler);
  }

  //ContextHandlerCollection несет доп. логику по маршрутизации внутри коллекции. Поэтому не заменяем ServletContextHandler на него
  JettyServer(FileSettings jettySettings, ContextHandlerCollection mutableHandlerCollectionForTestRun) {
    this.jettySettings = jettySettings;
    server = new Server();
    configureRequestLogger();
    configureStopTimeout();
    this.servletContextHandler = null;
    handlerCollection = mutableHandlerCollectionForTestRun;
    server.setHandler(mutableHandlerCollectionForTestRun);
  }

  public void start() throws JettyServerException {
    try {
      server.start();
      server.setStopAtShutdown(true);

      LOGGER.info("Jetty started on port {}", getPort());
    } catch (ConsulServiceException e) {
      stopSilently();
      throw e;
    } catch (Exception e) {
      stopSilently();
      String msg = ofNullable(jettySettings.getInteger(PORT)).filter(port -> port != 0).map(port -> ", port=" + port).orElse("");
      throw new JettyServerException("Unable to start Jetty server" + msg, e);
    }
  }

  public void stop() throws JettyServerException {
    try {
      server.stop();
      LOGGER.info("Jetty stopped");
    } catch (Exception e) {
      throw new JettyServerException("Unable to stop Jetty server", e);
    }
  }

  public int getPort() {
    Optional<ServerConnector> serverConnector = getServerConnector();
    if (serverConnector.isEmpty()) {
      LOGGER.warn("Unable to obtain port number - server connector is not present");
      return 0;
    }
    return serverConnector.get().getLocalPort();
  }

  public boolean isRunning() {
    return server.isRunning();
  }

  private void configureRequestLogger() {
    server.setRequestLog(new StructuredRequestLogger());
  }

  private void configureStopTimeout() {
    server.setStopTimeout(jettySettings.getInteger(STOP_TIMEOUT_SIZE, 5_000));
  }


  private Optional<ServerConnector> getServerConnector() {
    Connector[] connectors = server.getConnectors();
    for (Connector connector : connectors) {
      if (connector instanceof ServerConnector) {
        return Optional.of((ServerConnector) connector);
      }
    }
    return Optional.empty();
  }

  private void stopSilently() {
    try {
      server.stop();
    } catch (Exception ignored) {
    }
  }

  public Server getServer() {
    return server;
  }

  public ServletContext getServletContext() {
    return servletContextHandler != null ? servletContextHandler.getServletContext()
      : handlerCollection.getBean(ServletContextHandler.class).getServletContext();
  }
}
