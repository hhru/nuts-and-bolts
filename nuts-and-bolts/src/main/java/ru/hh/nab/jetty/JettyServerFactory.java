package ru.hh.nab.jetty;

import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import ru.hh.jetty.RequestLogger;
import ru.hh.nab.Settings;
import ru.hh.nab.Settings.IntProperty;
import ru.hh.nab.jersey.JerseyHttpServlet;
import ru.hh.nab.scopes.RequestScopeFilter;
import javax.servlet.DispatcherType;
import java.lang.management.ManagementFactory;
import java.util.EnumSet;
import java.util.Properties;

public abstract class JettyServerFactory {

  private static final IntProperty MIN_THREADS = new IntProperty("minThreads", 8);
  private static final IntProperty MAX_THREADS = new IntProperty("maxThreads", 128);

  private static final IntProperty OUTPUT_BUFFER_SIZE = new IntProperty("outputBufferSize", 65536);
  private static final IntProperty REQUEST_HEADER_SIZE = new IntProperty("requestHeaderSize", 16384);
  private static final IntProperty RESPONSE_HEADER_SIZE = new IntProperty("responseHeaderSize", 65536);
  private static final IntProperty HEADER_CACHE_SIZE = new IntProperty("headerCacheSize", 512);
  private static final IntProperty BLOCKING_TIMEOUT_MS = new IntProperty("blockingTimeout", 5000);

  private static final IntProperty ACCEPTORS = new IntProperty("acceptors", 1);
  private static final IntProperty SELECTORS = new IntProperty("selectors", -1);

  private static final IntProperty IDLE_THREAD_TIMEOUT_MS = new IntProperty("idleThreadTimeoutMs", 60000);
  private static final IntProperty IDLE_CONNECTION_TIMEOUT_MS = new IntProperty("idleConnectionTimeoutMs", 30000);
  private static final IntProperty ACCEPT_QUEUE_SIZE = new IntProperty("acceptQueueSize", 50);

  private static final IntProperty SERVER_STOP_TIMEOUT = new IntProperty("serverStopTimeout", 5000);

  public static Server create(Settings settings, JerseyHttpServlet jerseyHttpServlet) {

    final Properties properties = settings.subTree("jetty");

    HttpConfiguration httpConfiguration = new HttpConfiguration();
    httpConfiguration.setOutputBufferSize(OUTPUT_BUFFER_SIZE.from(properties));
    httpConfiguration.setRequestHeaderSize(REQUEST_HEADER_SIZE.from(properties));
    httpConfiguration.setResponseHeaderSize(RESPONSE_HEADER_SIZE.from(properties));
    httpConfiguration.setHeaderCacheSize(HEADER_CACHE_SIZE.from(properties));
    httpConfiguration.setBlockingTimeout(BLOCKING_TIMEOUT_MS.from(properties));

    httpConfiguration.setSendServerVersion(false);
    httpConfiguration.setSendDateHeader(true);

    final ThreadPool threadPool = new QueuedThreadPool(
      MAX_THREADS.from(properties),
      MIN_THREADS.from(properties),
      IDLE_THREAD_TIMEOUT_MS.from(properties)
    );

    final Server server = new Server(threadPool);

    final MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
    server.addEventListener(mbContainer);
    server.addBean(mbContainer);
    server.addBean(Log.getLog());

    server.setRequestLog(new RequestLogger());

    final HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfiguration);

    final ServerConnector serverConnector = new ServerConnector(
      server,
      ACCEPTORS.from(properties),
      SELECTORS.from(properties),
      httpConnectionFactory
    );
    serverConnector.setPort(settings.port);
    serverConnector.setIdleTimeout(IDLE_CONNECTION_TIMEOUT_MS.from(properties));
    serverConnector.setAcceptQueueSize(ACCEPT_QUEUE_SIZE.from(properties));

    server.addConnector(serverConnector);

    server.setStopAtShutdown(true);
    server.setStopTimeout(SERVER_STOP_TIMEOUT.from(properties));

    configureJerseyServlet(server, jerseyHttpServlet);

    return server;
  }

  private static Server configureJerseyServlet(Server server, JerseyHttpServlet jerseyHttpServlet) {
    WebAppContext context = new WebAppContext();
    context.setContextPath(JerseyHttpServlet.BASE_PATH);
    context.setResourceBase("/bad/local/system/path");
    context.addFilter(RequestScopeFilter.class, JerseyHttpServlet.MAPPING, EnumSet.allOf(DispatcherType.class));
    ServletHolder sh = new ServletHolder();
    sh.setServlet(jerseyHttpServlet);
    sh.setAsyncSupported(true);
    context.addServlet(sh, JerseyHttpServlet.MAPPING);
    server.setHandler(context);
    return server;
  }
}
