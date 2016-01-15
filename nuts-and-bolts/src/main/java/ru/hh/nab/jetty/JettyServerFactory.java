package ru.hh.nab.jetty;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import ru.hh.jetty.RequestLogger;
import ru.hh.nab.Settings;
import java.util.Properties;

public abstract class JettyServerFactory {

  public static int MIN_SERVER_THREADS = 16;
  public static int MAX_SERVER_THREADS = 128;

  private static int limitMinMax(final int value, final int min, final int max) {
    return Math.max(max, Math.min(min, value));
  }

  public static Server create(Settings settings) {

    // common settings for jetty and grizzly
    final int minThreads = limitMinMax(settings.concurrencyLevel, MIN_SERVER_THREADS, MAX_SERVER_THREADS);
    final int maxThreads = settings.workersQueueLimit == -1 ? minThreads :
      limitMinMax(settings.workersQueueLimit, minThreads, MAX_SERVER_THREADS);
    final int port = settings.port;

    Properties jettyProperties = settings.subTree("jetty");
    final int securePort = Integer.parseInt(jettyProperties.getProperty("securePort", "8443"));
    final int outputBufferSize = Integer.parseInt(jettyProperties.getProperty("outputBufferSize", "65536"));
    final int requestHeaderSize = Integer.parseInt(jettyProperties.getProperty("requestHeaderSize", "16384"));
    final int responseHeaderSize = Integer.parseInt(jettyProperties.getProperty("responseHeaderSize", "65536"));
    final boolean sendServerVersion = Boolean.parseBoolean(jettyProperties.getProperty("sendServerVersion", "false"));
    final boolean sendDateHeader = Boolean.parseBoolean(jettyProperties.getProperty("sendDateHeader", "true"));
    final int headerCacheSize = Integer.parseInt(jettyProperties.getProperty("headerCacheSize", "512"));
    final int blockingTimeout = Integer.parseInt(jettyProperties.getProperty("blockingTimeout", "5000"));

    final int acceptors = Integer.parseInt(jettyProperties.getProperty("acceptors", "2"));
    final int selectors = Integer.parseInt(jettyProperties.getProperty("selectors", "-1"));
    final int idleThreadTimeoutMs = Integer.parseInt(jettyProperties.getProperty("idleThreadTimeoutMs", "60000"));
    final int idleConnectionTimeoutMs = Integer.parseInt(jettyProperties.getProperty("idleConnectionTimeoutMs", "30000"));

    final int serverStopTimeout = Integer.parseInt(jettyProperties.getProperty("serverStopTimeout", "5000"));

    HttpConfiguration httpConfiguration = new HttpConfiguration();
    httpConfiguration.setSecurePort(securePort);
    httpConfiguration.setOutputBufferSize(outputBufferSize);
    httpConfiguration.setRequestHeaderSize(requestHeaderSize);
    httpConfiguration.setResponseHeaderSize(responseHeaderSize);
    httpConfiguration.setSendServerVersion(sendServerVersion);
    httpConfiguration.setSendDateHeader(sendDateHeader);
    httpConfiguration.setHeaderCacheSize(headerCacheSize);
    httpConfiguration.setBlockingTimeout(blockingTimeout);

    final ThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads, idleThreadTimeoutMs);
    final Server server = new Server(threadPool);
    server.setRequestLog(new RequestLogger());

    final HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfiguration);

    final ServerConnector serverConnector = new ServerConnector(server, acceptors, selectors, httpConnectionFactory);
    serverConnector.setPort(port);
    serverConnector.setIdleTimeout(idleConnectionTimeoutMs);

    server.addConnector(serverConnector);

    server.setStopAtShutdown(true);
    server.setStopTimeout(serverStopTimeout);
    return server;
  }
}
