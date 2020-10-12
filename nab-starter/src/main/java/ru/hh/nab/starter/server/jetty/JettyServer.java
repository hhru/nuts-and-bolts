package ru.hh.nab.starter.server.jetty;

import static java.util.Optional.ofNullable;

import javax.servlet.ServletContext;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.nab.common.properties.FileSettings;

import java.util.Optional;

import ru.hh.nab.starter.exceptions.ConsulServiceException;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.ACCEPTORS;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.ACCEPT_QUEUE_SIZE;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.CONNECTION_IDLE_TIMEOUT_MS;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.HOST;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.OUTPUT_BUFFER_SIZE;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.PORT;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.REQUEST_HEADER_SIZE;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.RESPONSE_HEADER_SIZE;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.SECURE_PORT;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.SELECTORS;
import static ru.hh.nab.starter.server.jetty.JettySettingsConstants.STOP_TIMEOUT_SIZE;
import ru.hh.nab.starter.server.logging.StructuredRequestLogger;

public final class JettyServer {
  private static final Logger LOGGER = LoggerFactory.getLogger(JettyServer.class);

  private final FileSettings jettySettings;
  private final Server server;
  private final ServletContextHandler servletContextHandler;
  private final ContextHandlerCollection handlerCollection;

  JettyServer(ThreadPool threadPool, FileSettings jettySettings, ServletContextHandler servletContextHandler) {
    this.jettySettings = jettySettings;
    server = new Server(threadPool);
    configureConnector();
    configureRequestLogger();
    configureStopTimeout();
    this.servletContextHandler = servletContextHandler;
    handlerCollection = null;
    server.setHandler(servletContextHandler);
  }

  //ContextHandlerCollection несет доп. логику по маршрутизации внутри коллекции. Поэтому не заменяем ServletContextHandler на него
  JettyServer(ThreadPool threadPool, FileSettings jettySettings, ContextHandlerCollection mutableHandlerCollectionForTestRun) {
    this.jettySettings = jettySettings;
    server = new Server(threadPool);
    configureConnector();
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

  private void configureConnector() {
    ServerConnector serverConnector = new HHServerConnector(
        server,
        ofNullable(jettySettings.getInteger(ACCEPTORS)).orElse(-1),
        ofNullable(jettySettings.getInteger(SELECTORS)).orElse(-1),
        createHttpConnectionFactory(jettySettings));

    serverConnector.setHost(jettySettings.getString(HOST));
    serverConnector.setPort(jettySettings.getInteger(PORT));
    serverConnector.setIdleTimeout(ofNullable(jettySettings.getInteger(CONNECTION_IDLE_TIMEOUT_MS)).orElse(3_000));
    serverConnector.setAcceptQueueSize(ofNullable(jettySettings.getInteger(ACCEPT_QUEUE_SIZE)).orElse(50));

    server.addConnector(serverConnector);
  }

  private void configureRequestLogger() {
    server.setRequestLog(new StructuredRequestLogger());
  }

  private void configureStopTimeout() {
    server.setStopTimeout(ofNullable(jettySettings.getInteger(STOP_TIMEOUT_SIZE)).orElse(5_000));
  }

  private static HttpConnectionFactory createHttpConnectionFactory(FileSettings jettySettings) {
    final HttpConfiguration httpConfiguration = new HttpConfiguration();
    httpConfiguration.setSecurePort(Optional.ofNullable(jettySettings.getInteger(SECURE_PORT)).orElse(8443));
    httpConfiguration.setOutputBufferSize(Optional.ofNullable(jettySettings.getInteger(OUTPUT_BUFFER_SIZE)).orElse(65536));
    httpConfiguration.setRequestHeaderSize(Optional.ofNullable(jettySettings.getInteger(REQUEST_HEADER_SIZE)).orElse(16384));
    httpConfiguration.setResponseHeaderSize(Optional.ofNullable(jettySettings.getInteger(RESPONSE_HEADER_SIZE)).orElse(65536));
    httpConfiguration.setSendServerVersion(false);
    // я не понимаю как таймаут можно заменить на темп.
    // org.eclipse.jetty.server.HttpConfiguration.setMinResponseDataRate будет отрывать соединение не через 5 секунд, а уже после первой передачи,
    // если темп в пересчете на секунду окажется меньше, чем указано. какое-то говно
    httpConfiguration.setBlockingTimeout(5000);
    return new HttpConnectionFactory(httpConfiguration);
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
